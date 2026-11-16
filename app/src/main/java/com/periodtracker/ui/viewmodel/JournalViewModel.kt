package com.periodtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.periodtracker.AppContainer
import com.periodtracker.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate

data class JournalUiState(val journal: JournalSnapshot? = null, val today: LocalDate = LocalDate.now(),
    val busy: Boolean = false, val error: String? = null)

class JournalViewModel(private val dependencies: AppContainer) : ViewModel() {
    private val mutable = MutableStateFlow(JournalUiState())
    val state = mutable.asStateFlow()
    private val notices = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = notices.asSharedFlow()
    private var observation: Job? = null
    init {
        retryLoading()
        viewModelScope.launch { while (true) { delay(15_000); if (state.value.today != LocalDate.now()) refreshDate() } }
    }
    fun retryLoading() {
        observation?.cancel()
        mutable.update { it.copy(error = null) }
        observation = viewModelScope.launch {
            dependencies.observe().catch { mutable.update { it.copy(error = "Your records could not be loaded. Restart the app to try again.") } }
                .collect { data ->
                    mutable.update { it.copy(journal = data) }
                    dependencies.reminders.schedule(data)
                }
        }
    }
    fun refreshDate() { mutable.update { it.copy(today = LocalDate.now()) }; state.value.journal?.let(dependencies.reminders::schedule) }
    fun clearError() { mutable.update { it.copy(error = null) } }
    private fun change(message: String, done: () -> Unit = {}, action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { action(); notices.emit(message); done() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = if (e is IllegalArgumentException) e.message else "Could not save this change. Your previous records are still available.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun savePeriod(period: Period, done: () -> Unit) = change("Period saved", done) { dependencies.savePeriod(period) }
    fun deletePeriod(id: String, done: () -> Unit) = change("Period deleted; daily logs retained", done) { dependencies.deletePeriod(id) }
    fun saveLog(log: DailyLog, done: () -> Unit) = change("Daily log saved", done) { dependencies.saveLog(log) }
    fun deleteLog(date: LocalDate, done: () -> Unit) = change("Daily log deleted", done) { dependencies.deleteLog(date) }
    fun preferences(value: UserPreferences, done: () -> Unit = {}) = change("Preferences saved", done) { dependencies.preferences(value) }
    fun onboarding(value: UserPreferences, period: Period?) = change("Welcome to Cycle Journal") { dependencies.onboarding(value, period) }
    fun deleteAll() = change("All local data deleted") { dependencies.reminders.cancel(); dependencies.deleteAll() }
}
