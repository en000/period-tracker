package com.periodtracker.ui.period

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.periodtracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*
import com.periodtracker.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.periodtracker.ui.viewmodel.PeriodEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PeriodEditor(existing: Period?, initialDate: LocalDate, finish: Boolean,
    data: JournalSnapshot, busy: Boolean, vm: JournalViewModel, close: () -> Unit) {
    val owner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
    val form: PeriodEditorViewModel = viewModel()
    form.initialize(existing, initialDate, finish, LocalDate.now())
    val values by form.values.collectAsStateWithLifecycle()
    val start = values[0]
    val end = values[1]
    val status = values[2]
    val id = form.id
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var unusual by remember { mutableStateOf(false) }
    val changed = form.changed
    fun dismiss() { if (!busy) { if (changed) discard = true else close() } }
    fun save(confirmed: Boolean = false) {
        when (val result = form.draft.validate(id, data.periods, LocalDate.now())) {
            is PeriodDraftResult.Invalid -> error = result.message
            is PeriodDraftResult.Valid -> if (result.requiresConfirmation && !confirmed) unusual = true else vm.savePeriod(result.period, close)
        }
    }
    BackHandler { dismiss() }
    ModalBottomSheet(onDismissRequest = { dismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (finish) "End Period" else if (existing == null) "Record period" else "Edit period", style = MaterialTheme.typography.headlineSmall)
            DateField("First day", start, { form.start(it); error = null })
            Choices("Status", PeriodStatus.entries, { it.name == status }, { when (it) {
                PeriodStatus.COMPLETED -> "Completed"; PeriodStatus.ONGOING -> "Still ongoing"; PeriodStatus.END_UNKNOWN -> "End date unknown"
            } }) { form.status(it); error = null }
            if (status == PeriodStatus.COMPLETED.name) DateField("Last bleeding day (inclusive)", end, { form.end(it); error = null })
            Text(stringResource(R.string.ui_period_dates_do_not_change_your_independent_daily_logs), style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Action("Save period", !busy) { save() }
            if (existing != null) TextButton(onClick = { delete = true }, enabled = !busy) { Text(stringResource(R.string.ui_delete_period)) }
            TextButton(onClick = { dismiss() }, enabled = !busy) { Text(stringResource(R.string.ui_cancel)) }
        }
    }
    if (discard) Confirm("Discard changes?", "Your unsaved period edits will be lost.", "Discard", { discard = false }, close)
    if (delete) Confirm("Delete period?", "This removes the period record. Daily logs will be kept.", "Delete", { delete = false }) { vm.deletePeriod(id, close) }
    if (unusual) Confirm("Check the dates", "This creates a shorter or longer interval than the usual setup range. Keep these recorded dates?", "Keep dates", { unusual = false }) { unusual = false; save(true) }
    }
}
