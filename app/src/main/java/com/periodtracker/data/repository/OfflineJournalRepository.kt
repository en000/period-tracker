package com.periodtracker.data.repository

import androidx.room.withTransaction
import com.periodtracker.data.backup.JournalJson
import com.periodtracker.data.source.*
import com.periodtracker.domain.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class OfflineJournalRepository(private val database: JournalDatabase,
    private val preferences: PreferencesDataSource) : JournalRepository {
    private val dao = database.journalDao()
    private val writes = Mutex()
    override val snapshots = combine(dao.observeChanges(), preferences.preferences) { _, _ ->
        // Avoid mixed snapshots from separate table flows during imports and multi-table edits.
        writes.withLock { read() }
    }
    override suspend fun snapshot(): JournalSnapshot = writes.withLock { read() }
    private suspend fun read(): JournalSnapshot = database.withTransaction {
        JournalSnapshot(dao.periods().map { it.domain() }, dao.logs().map { it.domain() },
            dao.pending()?.let { JournalJson.decodePreferences(it.json) } ?: preferences.read())
    }
    override suspend fun savePeriod(period: Period) = writes.withLock {
        database.withTransaction {
            val existing = dao.periods()
            PeriodValidation.validate(existing.filterNot { it.id == period.id }.map { it.domain() } + period, LocalDate.now())
            val now = System.currentTimeMillis()
            dao.putPeriod(period.entity(existing.find { it.id == period.id }?.createdAt ?: now, now))
        }
    }
    override suspend fun deletePeriod(id: String) = writes.withLock { dao.deletePeriod(id) }
    override suspend fun saveLog(log: DailyLog) = writes.withLock {
        log.validate(LocalDate.now())
        database.withTransaction {
            if (log.isEmpty) { dao.deleteLog(log.date.toEpochDay()); return@withTransaction }
            val now = System.currentTimeMillis()
            val created = dao.log(log.date.toEpochDay())?.log?.createdAt ?: now
            putLog(log.copy(createdAt = created, updatedAt = now))
        }
    }
    override suspend fun deleteLog(date: LocalDate) = writes.withLock { dao.deleteLog(date.toEpochDay()) }
    override suspend fun setPreferences(preferences: UserPreferences) = writes.withLock {
        preferences.validate()
        dao.putPending(PendingPreferences(json = JournalJson.encodePreferences(preferences)))
        flushPreferences()
    }
    private suspend fun flushPreferences() {
        val pending = dao.pending() ?: return
        // If the second store is temporarily unavailable, the committed Room journal remains authoritative.
        try { preferences.write(JournalJson.decodePreferences(pending.json)); dao.clearPending() }
        catch (_: java.io.IOException) { /* Replayed on next write; no committed settings are lost. */ }
    }
    override suspend fun replace(snapshot: JournalSnapshot) = writes.withLock { replaceUnlocked(snapshot) }
    private suspend fun replaceUnlocked(snapshot: JournalSnapshot) {
        JournalJson.validate(snapshot, LocalDate.now())
        database.withTransaction {
            dao.clearLogs(); dao.clearPeriods(); dao.clearReceipts()
            snapshot.periods.forEach { dao.putPeriod(it.entity(it.createdAt, it.updatedAt)) }
            snapshot.logs.forEach { putLog(it) }
            dao.putPending(PendingPreferences(json = JournalJson.encodePreferences(snapshot.preferences)))
        }
        flushPreferences()
    }
    override suspend fun completeOnboarding(preferences: UserPreferences, period: Period?) = writes.withLock {
        val old = read()
        require(!old.preferences.onboardingComplete) { "Setup is already complete." }
        val now = System.currentTimeMillis()
        replaceUnlocked(old.copy(preferences = preferences.copy(onboardingComplete = true, draftStart = "", draftEnd = ""),
            periods = old.periods + listOfNotNull(period?.copy(createdAt = now, updatedAt = now))))
    }
    override suspend fun claimReminder(key: String): Boolean = writes.withLock { dao.claim(ReminderReceipt(key)) != -1L }
    private suspend fun putLog(log: DailyLog) {
        val date = log.date.toEpochDay()
        dao.putLog(DailyLogEntity(date, log.flow?.name, log.mood?.name, log.pain?.name, log.notes,
            log.noSymptomsExplicit, log.createdAt, log.updatedAt))
        dao.clearSymptoms(date)
        dao.putSymptoms(log.symptoms.map { SymptomEntity(date, it.name) })
    }
}
private fun PeriodEntity.domain() = Period(id, LocalDate.ofEpochDay(start), end?.let(LocalDate::ofEpochDay),
    PeriodStatus.valueOf(status), createdAt, updatedAt)
private fun Period.entity(created: Long, updated: Long) = PeriodEntity(id, start.toEpochDay(), end?.toEpochDay(), status.name, created, updated)
private fun LogWithSymptoms.domain() = DailyLog(LocalDate.ofEpochDay(log.date), log.flow?.let(FlowLevel::valueOf),
    log.mood?.let(Mood::valueOf), log.pain?.let(Pain::valueOf), log.notes, symptoms.map { Symptom.valueOf(it.code) }.toSet(),
    log.noSymptomsExplicit, log.createdAt, log.updatedAt)
