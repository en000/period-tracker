package com.periodtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.periodtracker.AppContainer
import com.periodtracker.domain.JournalSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(val busy: Boolean = false, val preview: JournalSnapshot? = null, val message: String? = null)
class BackupViewModel(private val dependencies: AppContainer) : ViewModel() {
    private val mutable = MutableStateFlow(BackupUiState())
    val state = mutable.asStateFlow()
    fun dismiss() { if (!state.value.busy) mutable.value = BackupUiState() }
    fun export(uri: String, password: CharArray) = run {
        dependencies.export(uri, password); BackupUiState(message = "Encrypted backup exported successfully.")
    }
    fun preview(uri: String, password: CharArray) = run {
        BackupUiState(preview = dependencies.preview(uri, password))
    }
    fun restore() {
        val snapshot = state.value.preview ?: return
        run { dependencies.restore(snapshot); dependencies.reminders.schedule(snapshot); BackupUiState(message = "Backup restored.") }
    }
    private fun run(action: suspend () -> BackupUiState) {
        if (state.value.busy) return
        mutable.value = state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { mutable.value = action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = BackupUiState(message = backupFailureMessage(e)) }
        }
    }
}
