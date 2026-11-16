package com.periodtracker.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.periodtracker.R
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*
import com.periodtracker.ui.settings.*
import com.periodtracker.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.util.UUID

@Composable fun OnboardingView(p: UserPreferences, busy: Boolean, vm: JournalViewModel) {
    var start by rememberSaveable { mutableStateOf(p.draftStart) }
    var end by rememberSaveable { mutableStateOf(p.draftEnd) }
    var length by rememberSaveable { mutableStateOf(p.cycle.usualCycleLength.toString()) }
    var duration by rememberSaveable { mutableStateOf(p.cycle.usualPeriodDuration.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = length.toIntOrNull() in 15..90 && duration.toIntOrNull() in 1..15
    BackHandler(p.onboardingStep > 0) { vm.preferences(p.copy(onboardingStep = p.onboardingStep - 1)) }
    Page("Cycle Journal") {
        Text("${p.onboardingStep + 1} / 3", style = MaterialTheme.typography.labelLarge)
        when (p.onboardingStep) {
            0 -> {
                Text(stringResource(R.string.ui_a_little_space_for_your_cycle), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.ui_record_your_period_and_how_you_feel_see_your_history_at_your_own_pace))
                Text(stringResource(R.string.ui_private_and_offline_your_data_stays_on_your_device_unless_you_choose_to))
                Text(stringResource(R.string.estimate_disclaimer))
                Action("Get started", !busy) { vm.preferences(p.copy(onboardingStep = 1)) }
            }
            1 -> {
                Text(stringResource(R.string.ui_your_usual_rhythm), style = MaterialTheme.typography.headlineMedium)
                DateField("Last period start (optional)", start, { start = it }, optional = true)
                if (start.isNotBlank()) DateField("Actual last day (optional)", end, { end = it }, optional = true)
                Text(stringResource(R.string.ui_if_you_leave_the_end_date_blank_only_the_start_is_recorded_it_will_not_b))
                OutlinedTextField(length, { length = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_usual_cycle_length_15_90_days)) }, singleLine = true)
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_usual_period_duration_1_15_days)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Action("Continue", valid && !busy) {
                    if (start.isNotBlank() && end.isNotBlank() && LocalDate.parse(end) < LocalDate.parse(start)) error = "End date must be on or after the start date."
                    else vm.preferences(p.copy(onboardingStep = 2, draftStart = start, draftEnd = if (start.isBlank()) "" else end,
                        cycle = CyclePreferences(length.toInt(), duration.toInt())))
                }
            }
            2 -> {
                Text(stringResource(R.string.ui_make_it_yours), style = MaterialTheme.typography.headlineMedium)
                AppearanceChoices(p.appearance) { vm.preferences(p.copy(appearance = it)) }
                ReminderPreferences(p) { vm.preferences(it) }
                Text(stringResource(R.string.ui_you_can_export_a_password_encrypted_backup_in_settings_whenever_you_want))
                Action("Start journaling", !busy) {
                    val period = p.draftStart.takeIf(String::isNotBlank)?.let { date -> Period(UUID.randomUUID().toString(), LocalDate.parse(date),
                        p.draftEnd.takeIf(String::isNotBlank)?.let(LocalDate::parse), if (p.draftEnd.isBlank()) PeriodStatus.END_UNKNOWN else PeriodStatus.COMPLETED) }
                    vm.onboarding(p, period)
                }
            }
        }
        if (p.onboardingStep > 0) TextButton(onClick = { vm.preferences(p.copy(onboardingStep = p.onboardingStep - 1)) }, enabled = !busy) { Text(stringResource(R.string.ui_back)) }
    }
}
