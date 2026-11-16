package com.periodtracker.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.periodtracker.R
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*

@Composable fun InsightsView(data: JournalSnapshot, edit: (Period) -> Unit, openLog: (java.time.LocalDate) -> Unit) {
    val stats = CycleStatisticsEngine.calculate(data.periods)
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    Page(stringResource(R.string.insights)) {
        if (data.periods.isEmpty()) Section("Your history starts here") { Text(stringResource(R.string.ui_record_a_period_in_calendar_to_begin_statistics_appear_as_you_add_record)) }
        Section("Your recorded patterns") {
            Text("Typical cycle length: ${stats.typicalCycle?.let { "$it days" } ?: "Not enough data"}")
            Text("Typical period duration: ${stats.typicalPeriod?.let { "$it days" } ?: "No completed periods"}")
            if (stats.intervalCount == 1) Text(stringResource(R.string.ui_one_observed_interval_more_history_is_needed_to_describe_a_trend))
            if (stats.shortestCycle != null) Text("Shortest cycle: ${stats.shortestCycle} days\nLongest cycle: ${stats.longestCycle} days")
        }
        if (stats.recentIntervals.isNotEmpty()) Section("Recent cycle lengths") {
            val pairs = data.periods.sortedBy { it.start }.zipWithNext().takeLast(6)
            val max = stats.recentIntervals.max().toFloat()
            pairs.forEach { (a, b) ->
                val days = daysBetween(a.start, b.start)
                Column(Modifier.fillMaxWidth().semantics { contentDescription = "${dateLabel(a.start)}: $days days" }) {
                    Text("${dateLabel(a.start)} · $days days", style = MaterialTheme.typography.labelLarge)
                    Box(Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth((days / max).coerceIn(0.01f, 1f)).height(16.dp).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
        Section("Cycle history") {
            Text(stringResource(R.string.ui_newest_first_select_a_record_to_see_its_details_and_daily_logs))
            data.periods.sortedByDescending { it.start }.forEach { p ->
                OutlinedButton(onClick = { detail = p.id }, modifier = Modifier.fillMaxWidth()) {
                    Text("${dateLabel(p.start)} · ${p.end?.let(::dateLabel) ?: if (p.status == PeriodStatus.ONGOING) "Ongoing" else "End unknown"}")
                }
            }
        }
        Text(stringResource(R.string.nonmedical), style = MaterialTheme.typography.bodySmall)
    }
    val selected = data.periods.find { it.id == detail }
    if (selected != null) {
        val next = data.periods.filter { it.start > selected.start }.minByOrNull { it.start }
        val logs = data.logs.filter { it.date >= selected.start && (next == null || it.date < next.start) }
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(dateLabel(selected.start)) }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status: ${enumLabel(selected.status)}")
                selected.completedDuration?.let { Text("Period duration: $it days (inclusive)") }
                next?.let { Text("Cycle length: ${daysBetween(selected.start, it.start)} days") }
                if (logs.isEmpty()) Text(stringResource(R.string.ui_no_daily_logs_in_this_cycle))
                logs.forEach { log -> TextButton(onClick = { detail = null; openLog(log.date) }) { Text("${dateLabel(log.date)} · ${log.flow?.let(::enumLabel) ?: "Daily log"}") } }
            }
        }, confirmButton = { TextButton(onClick = { detail = null; edit(selected) }) { Text(stringResource(R.string.ui_edit_period)) } },
            dismissButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.ui_close)) } })
    }
}
