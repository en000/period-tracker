package com.periodtracker.ui.daydetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.periodtracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*
import com.periodtracker.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.*
import com.periodtracker.ui.viewmodel.DailyLogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DayDetailsView(date: LocalDate, data: JournalSnapshot, busy: Boolean, vm: JournalViewModel,
    close: () -> Unit, editPeriod: (Period?, Boolean) -> Unit) {
    val existing = data.logs.find { it.date == date }
    val owner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
    val form: DailyLogViewModel = viewModel()
    form.initialize(date, existing)
    val fields by form.fields.collectAsStateWithLifecycle()
    val symptoms by form.symptoms.collectAsStateWithLifecycle()
    val none by form.noSymptoms.collectAsStateWithLifecycle()
    val flow = fields[0]
    val mood = fields[1]
    val pain = fields[2]
    val notes = fields[3]
    var discard by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var afterDiscard by remember { mutableStateOf<(() -> Unit)?>(null) }
    val draft = form.draft()
    val changed = form.changed
    fun leave(action: () -> Unit) { if (!busy) { if (changed) { afterDiscard = action; discard = true } else action() } }
    val future = date > LocalDate.now()
    val period = data.periods.find { it.contains(date, LocalDate.now()) }
    val ongoing = data.periods.find { it.status == PeriodStatus.ONGOING }
    BackHandler { leave(close) }
    ModalBottomSheet(onDismissRequest = { leave(close) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(dateLabel(date), style = MaterialTheme.typography.headlineSmall)
            cycleDay(data.periods, date)?.let { Text("Cycle day $it", style = MaterialTheme.typography.bodyMedium) }
            if (future) Text(stringResource(R.string.ui_future_dates_are_for_viewing_estimates_observations_can_be_saved_on_or_a))
            Choices("Flow", listOf("") + FlowLevel.entries.map { it.name }, { it == flow }, { if (it.isEmpty()) "Unspecified" else enumLabel(FlowLevel.valueOf(it)) }) { form.field(0, it) }
            Choices("Symptoms", Symptom.entries, { it.name in symptoms }, ::enumLabel) {
                form.toggleSymptom(it)
            }
            FilterChip(none, form::toggleNone, label = { Text(stringResource(R.string.ui_no_symptoms)) })
            Choices("Mood", listOf("") + Mood.entries.map { it.name }, { it == mood }, { if (it.isEmpty()) "Unspecified" else enumLabel(Mood.valueOf(it)) }) { form.field(1, it) }
            Choices("Pain", listOf("") + Pain.entries.map { it.name }, { it == pain }, { if (it.isEmpty()) "Unspecified" else enumLabel(Pain.valueOf(it)) }) { form.field(2, it) }
            OutlinedTextField(notes, { form.field(3, it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_notes)) },
                supportingText = { Text("${notes.length}/2000") }, minLines = 3, maxLines = 6)
            Action("Save daily log", !busy && !future && changed && (!draft.isEmpty || existing != null)) {
                if (draft.isEmpty && existing != null) delete = true else vm.saveLog(draft, close)
            }
            if (existing != null) TextButton(onClick = { delete = true }, enabled = !busy) { Text(stringResource(R.string.ui_delete_daily_log)) }
            HorizontalDivider()
            if (!future) {
                if (period != null) OutlinedButton(onClick = { leave { editPeriod(period, false) } }, enabled = !busy) { Text(stringResource(R.string.ui_view_edit_period)) }
                else if (ongoing == null || date < ongoing.start) OutlinedButton(onClick = { leave { editPeriod(null, false) } }, enabled = !busy) { Text("Start period on ${dateLabel(date)}") }
                if (ongoing != null && date >= ongoing.start) OutlinedButton(onClick = { leave { editPeriod(ongoing, true) } }, enabled = !busy) { Text("End period on ${dateLabel(date)}") }
            }
            TextButton(onClick = { leave(close) }, enabled = !busy) { Text(stringResource(R.string.ui_close)) }
        }
    }
    if (discard) Confirm("Discard changes?", "Your unsaved daily log edits will be lost.", "Discard", { discard = false }, { discard = false; afterDiscard?.invoke() })
    if (delete) Confirm("Delete daily log?", "This removes the daily observations only. Period dates will be kept.", "Delete", { delete = false }) { vm.deleteLog(date, close) }
    }
}
