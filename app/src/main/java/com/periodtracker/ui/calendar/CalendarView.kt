package com.periodtracker.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.periodtracker.R
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration

@Composable fun CalendarView(data: JournalSnapshot, today: LocalDate, month: YearMonth,
    previous: () -> Unit, next: () -> Unit, goToday: () -> Unit, select: (LocalDate) -> Unit, periodAction: () -> Unit) {
    val engine = CyclePredictionEngine(Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC))
    val prediction = engine.predict(data.periods, data.preferences.cycle)
    val ongoing = data.periods.any { it.status == PeriodStatus.ONGOING }
    val locale = LocalConfiguration.current.locales[0]
    Page(stringResource(R.string.calendar)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ui_your_cycle), style = MaterialTheme.typography.labelLarge)
                val title = when (val status = engine.status(data.periods, data.preferences.cycle)) {
                    CycleStatus.NoHistory -> "Add your first period to see an estimate"
                    is CycleStatus.Ongoing -> "Period day ${status.day}"
                    is CycleStatus.AroundToday -> "Period estimated around today"
                    is CycleStatus.PreviousEstimate -> "Your previous estimate was ${dateLabel(status.prediction.centralStart)}"
                    is CycleStatus.Upcoming -> "Next period estimated in ~${status.days} days"
                }
                Text(title, style = MaterialTheme.typography.headlineMedium)
                if (!ongoing && prediction != null) {
                    Text("Estimated start: ${dateLabel(prediction.earliestStart)} – ${dateLabel(prediction.latestStart)}")
                    Text(when (prediction.basis) {
                        EstimateBasis.USUAL_LENGTH -> "Based on your usual cycle length"
                        EstimateBasis.PRELIMINARY -> "Preliminary estimate; limited recorded history"
                        EstimateBasis.RECORDED_HISTORY -> if (prediction.timingVaries) "Timing varies" else "Based on your recorded history"
                    }, style = MaterialTheme.typography.bodySmall)
                    if (prediction.centralStart < today) Text(stringResource(R.string.ui_record_a_period_when_it_starts))
                }
                if ((engine.status(data.periods, data.preferences.cycle) as? CycleStatus.Ongoing)?.reviewSuggested == true)
                    Text(stringResource(R.string.ui_this_record_is_still_ongoing_review_its_dates_when_you_are_ready))
                Text(stringResource(R.string.estimate_disclaimer), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(previous) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), style = MaterialTheme.typography.titleLarge)
            IconButton(next) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
        }
        if (month != YearMonth.from(today)) TextButton(goToday) { Text(stringResource(R.string.ui_today)) }
        val firstWeekday = WeekFields.of(locale).firstDayOfWeek
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { offset -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(firstWeekday.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale))
            } }
        }
        val leading = (month.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
        val rows = (leading + month.lengthOfMonth() + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rows) { row -> Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = row * 7 + column - leading + 1
                    if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(52.dp))
                    else {
                        val date = month.atDay(day)
                        val recorded = data.periods.any { it.contains(date, today) }
                        val predicted = !recorded && !ongoing && prediction?.contains(date) == true
                        val logged = data.logs.any { it.date == date }
                        val description = listOfNotNull(dateLabel(date), "Today".takeIf { date == today },
                            "Recorded period".takeIf { recorded }, "Predicted period".takeIf { predicted }, "Daily log available".takeIf { logged }).joinToString(", ")
                        Surface(onClick = { select(date) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).padding(2.dp).semantics { contentDescription = description },
                            shape = if (predicted) androidx.compose.foundation.shape.RoundedCornerShape(8.dp) else CircleShape,
                            color = if (recorded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (recorded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = if (predicted || date == today) BorderStroke(if (date == today) 2.dp else 1.dp, MaterialTheme.colorScheme.primary) else null) {
                            Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.toString()); Text(if (logged) "•" else if (predicted) "~" else "", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } }
        }
        Text("● Recorded period    ▢ ~ Predicted period    • Daily log\nRing: today", style = MaterialTheme.typography.bodySmall)
        Action(if (ongoing) "End Period" else "Start Period", onClick = periodAction)
    }
}
