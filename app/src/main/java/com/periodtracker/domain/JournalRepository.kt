package com.periodtracker.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

enum class Appearance { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val onboardingStep: Int = 0,
    val draftStart: String = "",
    val draftEnd: String = "",
    val cycle: CyclePreferences = CyclePreferences(),
    val appearance: Appearance = Appearance.SYSTEM,
    val upcomingEnabled: Boolean = false,
    val dailyEnabled: Boolean = false,
    val leadDays: Int = 2,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
    val discreet: Boolean = true,
) {
    fun validate() {
        cycle.validate()
        require(leadDays in listOf(1, 2, 3, 5)) { "Choose 1, 2, 3 or 5 days of notice." }
        require(onboardingStep in 0..2) { "Invalid setup step." }
    }
}

data class JournalSnapshot(
    val periods: List<Period> = emptyList(),
    val logs: List<DailyLog> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
)

interface JournalRepository {
    val snapshots: Flow<JournalSnapshot>
    suspend fun snapshot(): JournalSnapshot
    suspend fun savePeriod(period: Period)
    suspend fun deletePeriod(id: String)
    suspend fun saveLog(log: DailyLog)
    suspend fun deleteLog(date: java.time.LocalDate)
    suspend fun setPreferences(preferences: UserPreferences)
    suspend fun replace(snapshot: JournalSnapshot)
    suspend fun completeOnboarding(preferences: UserPreferences, period: Period?)
    suspend fun claimReminder(key: String): Boolean
}
