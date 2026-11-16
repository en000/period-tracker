package com.periodtracker.domain.usecase

import com.periodtracker.domain.*
import java.time.LocalDate

class ObserveJournal(private val repository: JournalRepository) { operator fun invoke() = repository.snapshots }
class SavePeriod(private val repository: JournalRepository) {
    suspend operator fun invoke(period: Period) = repository.savePeriod(period)
}
class DeletePeriod(private val repository: JournalRepository) { suspend operator fun invoke(id: String) = repository.deletePeriod(id) }
class SaveDailyLog(private val repository: JournalRepository) { suspend operator fun invoke(log: DailyLog) = repository.saveLog(log) }
class DeleteDailyLog(private val repository: JournalRepository) { suspend operator fun invoke(date: LocalDate) = repository.deleteLog(date) }
class UpdatePreferences(private val repository: JournalRepository) {
    suspend operator fun invoke(preferences: UserPreferences) = repository.setPreferences(preferences)
}
class FinishOnboarding(private val repository: JournalRepository) {
    suspend operator fun invoke(preferences: UserPreferences, period: Period?) = repository.completeOnboarding(preferences, period)
}
class DeleteAllData(private val repository: JournalRepository) { suspend operator fun invoke() = repository.replace(JournalSnapshot()) }
