package com.periodtracker

import android.app.Application
import androidx.room.Room
import com.periodtracker.data.repository.*
import com.periodtracker.data.source.*
import com.periodtracker.domain.JournalRepository
import com.periodtracker.domain.usecase.*
import com.periodtracker.reminders.ReminderScheduler

/** Explicit application-scoped dependency graph; no service lookups in views or domain code. */
class JournalApplication : Application() {
    val container by lazy { AppContainer(this) }
}
class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(application, JournalDatabase::class.java, "cycle-journal.db").build()
    val repository: JournalRepository = OfflineJournalRepository(database, PreferencesDataSource(application))
    val backupRepository: BackupRepository = EncryptedBackupRepository(repository, BackupDocumentDataSource(application.contentResolver))
    val reminders = ReminderScheduler(application)
    val observe = ObserveJournal(repository)
    val savePeriod = SavePeriod(repository)
    val deletePeriod = DeletePeriod(repository)
    val saveLog = SaveDailyLog(repository)
    val deleteLog = DeleteDailyLog(repository)
    val preferences = UpdatePreferences(repository)
    val onboarding = FinishOnboarding(repository)
    val deleteAll = DeleteAllData(repository)
    val export = ExportBackup(backupRepository)
    val preview = PreviewBackup(backupRepository)
    val restore = RestoreBackup(backupRepository)
}
