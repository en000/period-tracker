package com.periodtracker.data.source

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "periods", indices = [Index(value = ["start"], unique = true)])
data class PeriodEntity(@PrimaryKey val id: String, val start: Long, val end: Long?, val status: String,
    val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "daily_logs")
data class DailyLogEntity(@PrimaryKey val date: Long, val flow: String?, val mood: String?, val pain: String?,
    val notes: String, val noSymptomsExplicit: Boolean?, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "symptoms", primaryKeys = ["date", "code"], foreignKeys = [ForeignKey(
    entity = DailyLogEntity::class, parentColumns = ["date"], childColumns = ["date"], onDelete = ForeignKey.CASCADE)])
data class SymptomEntity(val date: Long, val code: String)

data class LogWithSymptoms(@Embedded val log: DailyLogEntity,
    @Relation(parentColumn = "date", entityColumn = "date") val symptoms: List<SymptomEntity>)

/** Durable cross-store commit journal: Room data and replacement preferences commit together. */
@Entity(tableName = "pending_preferences")
data class PendingPreferences(@PrimaryKey val id: Int = 1, val json: String)

@Entity(tableName = "reminder_receipts")
data class ReminderReceipt(@PrimaryKey val key: String)

@Dao
interface JournalDao {
    // One invalidation stream covers the entire dataset; callers read it in one transaction.
    @Query("SELECT (SELECT COUNT(*) FROM periods) + (SELECT COUNT(*) FROM daily_logs) + (SELECT COUNT(*) FROM symptoms) + (SELECT COUNT(*) FROM pending_preferences)")
    fun observeChanges(): Flow<Int>
    @Query("SELECT * FROM periods ORDER BY start") fun observePeriods(): Flow<List<PeriodEntity>>
    @Query("SELECT * FROM periods ORDER BY start") suspend fun periods(): List<PeriodEntity>
    @Transaction @Query("SELECT * FROM daily_logs ORDER BY date") fun observeLogs(): Flow<List<LogWithSymptoms>>
    @Transaction @Query("SELECT * FROM daily_logs ORDER BY date") suspend fun logs(): List<LogWithSymptoms>
    @Transaction @Query("SELECT * FROM daily_logs WHERE date = :date") suspend fun log(date: Long): LogWithSymptoms?
    @Query("SELECT * FROM pending_preferences WHERE id = 1") fun observePending(): Flow<PendingPreferences?>
    @Query("SELECT * FROM pending_preferences WHERE id = 1") suspend fun pending(): PendingPreferences?
    @Upsert suspend fun putPeriod(period: PeriodEntity)
    @Query("DELETE FROM periods WHERE id = :id") suspend fun deletePeriod(id: String)
    @Query("DELETE FROM periods") suspend fun clearPeriods()
    @Upsert suspend fun putLog(log: DailyLogEntity)
    @Insert suspend fun putSymptoms(symptoms: List<SymptomEntity>)
    @Query("DELETE FROM symptoms WHERE date = :date") suspend fun clearSymptoms(date: Long)
    @Query("DELETE FROM daily_logs WHERE date = :date") suspend fun deleteLog(date: Long)
    @Query("DELETE FROM daily_logs") suspend fun clearLogs()
    @Upsert suspend fun putPending(pending: PendingPreferences)
    @Query("DELETE FROM pending_preferences") suspend fun clearPending()
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claim(receipt: ReminderReceipt): Long
    @Query("DELETE FROM reminder_receipts") suspend fun clearReceipts()
}

@Database(entities = [PeriodEntity::class, DailyLogEntity::class, SymptomEntity::class,
    PendingPreferences::class, ReminderReceipt::class], version = 1, exportSchema = true)
abstract class JournalDatabase : RoomDatabase() { abstract fun journalDao(): JournalDao }
