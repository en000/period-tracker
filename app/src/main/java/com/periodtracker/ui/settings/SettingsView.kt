package com.periodtracker.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.periodtracker.R
import com.periodtracker.domain.*
import com.periodtracker.ui.components.*
import com.periodtracker.ui.viewmodel.*
import java.time.LocalTime

@Composable fun AppearanceChoices(value: Appearance, change: (Appearance) -> Unit) {
    Choices("Appearance", Appearance.entries, { it == value }, ::enumLabel, change)
}
@Composable fun ReminderPreferences(p: UserPreferences, change: (UserPreferences) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LifecycleResumeEffect(Unit) { allowed = NotificationManagerCompat.from(context).areNotificationsEnabled(); onPauseOrDispose {} }
    fun update(value: UserPreferences) {
        change(value)
        if (Build.VERSION.SDK_INT >= 33 && !allowed && (value.upcomingEnabled || value.dailyEnabled)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Toggle("Upcoming period reminder", p.upcomingEnabled) { update(p.copy(upcomingEnabled = it)) }
    if (p.upcomingEnabled) Choices("Advance notice", listOf(1, 2, 3, 5), { it == p.leadDays }, { "$it days" }) { change(p.copy(leadDays = it)) }
    Toggle("Daily logging reminder", p.dailyEnabled) { update(p.copy(dailyEnabled = it)) }
    OutlinedButton(onClick = {
        TimePickerDialog(context, { _, hour, minute -> change(p.copy(reminderTime = LocalTime.of(hour, minute))) },
            p.reminderTime.hour, p.reminderTime.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
    }) { Text("Reminder time: ${p.reminderTime}") }
    Toggle("Discreet notification text", p.discreet) { change(p.copy(discreet = it)) }
    Text(stringResource(R.string.ui_reminders_are_optional_and_may_be_delayed_by_android_daily_reminders_are), style = MaterialTheme.typography.bodySmall)
    if (!allowed && (p.upcomingEnabled || p.dailyEnabled)) {
        Text(stringResource(R.string.ui_notifications_are_blocked_by_android_reminders_cannot_appear_until_permi), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(stringResource(R.string.ui_open_notification_settings)) }
    }
}
@Composable fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, change)
    }
}
@Composable fun CyclePreferencesFields(cycle: CyclePreferences, save: (CyclePreferences) -> Unit) {
    var length by rememberSaveable(cycle.usualCycleLength) { mutableStateOf(cycle.usualCycleLength.toString()) }
    var duration by rememberSaveable(cycle.usualPeriodDuration) { mutableStateOf(cycle.usualPeriodDuration.toString()) }
    val valid = length.toIntOrNull() in 15..90 && duration.toIntOrNull() in 1..15
    OutlinedTextField(length, { length = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_usual_cycle_length_15_90_days)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = length.toIntOrNull() !in 15..90)
    OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_usual_period_duration_1_15_days)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = duration.toIntOrNull() !in 1..15)
    Text(stringResource(R.string.ui_recorded_history_takes_precedence_when_enough_dates_are_available_durati), style = MaterialTheme.typography.bodySmall)
    Action("Save cycle preferences", valid) { save(CyclePreferences(length.toInt(), duration.toInt())) }
}
@Composable fun SettingsView(p: UserPreferences, busy: Boolean, vm: JournalViewModel, backup: BackupViewModel) {
    var delete by remember { mutableStateOf(false) }
    var acknowledged by remember { mutableStateOf(false) }
    Page(stringResource(R.string.settings)) {
        Section("Cycle preferences") { CyclePreferencesFields(p.cycle) { vm.preferences(p.copy(cycle = it)) } }
        Section("Appearance") { AppearanceChoices(p.appearance) { vm.preferences(p.copy(appearance = it)) } }
        Section("Reminders") { ReminderPreferences(p) { vm.preferences(it) } }
        Section("Privacy & Data") {
            Text(stringResource(R.string.privacy_copy))
            BackupControls(backup)
            TextButton(onClick = { delete = true }, enabled = !busy) { Text(stringResource(R.string.ui_delete_all_data), color = MaterialTheme.colorScheme.error) }
        }
        Section("About Cycle Journal") {
            Text(stringResource(R.string.ui_version_1_0_working_name_and_visual_design))
            Text(stringResource(R.string.nonmedical))
            Text(stringResource(R.string.ui_built_with_kotlin_and_android_jetpack_androidx_libraries_are_licensed_un), style = MaterialTheme.typography.bodySmall)
        }
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false; acknowledged = false }, title = { Text(stringResource(R.string.ui_delete_all_local_data)) }, text = {
        Column { Text(stringResource(R.string.ui_periods_daily_logs_preferences_and_reminders_will_be_erased_exported_bac))
            Toggle("I understand this cannot be undone", acknowledged) { acknowledged = it } }
    }, confirmButton = { TextButton(onClick = { delete = false; acknowledged = false; backup.dismiss(); vm.deleteAll() }, enabled = acknowledged && !busy) { Text(stringResource(R.string.ui_delete_everything)) } },
        dismissButton = { TextButton(onClick = { delete = false; acknowledged = false }) { Text(stringResource(R.string.ui_cancel)) } })
}
@Composable fun BackupControls(vm: BackupViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    // Secrets deliberately are not saved in Android instance state.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri = it?.toString(); exporting = true }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri = it?.toString(); exporting = false }
    OutlinedButton(onClick = { vm.dismiss(); create.launch("cycle-journal.cyclebackup") }, enabled = !state.busy) { Text(stringResource(R.string.ui_export_encrypted_backup)) }
    OutlinedButton(onClick = { vm.dismiss(); open.launch(arrayOf("*/*")) }, enabled = !state.busy) { Text(stringResource(R.string.ui_import_encrypted_backup)) }
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.message?.let { Text(it) }
    if (uri != null) AlertDialog(onDismissRequest = { uri = null; password = ""; confirm = "" }, title = { Text(if (exporting) "Encrypt your backup" else "Unlock backup") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (exporting) "Use at least 10 characters. This passphrase cannot be recovered if forgotten." else "Enter the passphrase used when this backup was created.")
            OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.ui_passphrase)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (exporting) OutlinedTextField(confirm, { confirm = it }, label = { Text(stringResource(R.string.ui_repeat_passphrase)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
    }, confirmButton = { TextButton(enabled = if (exporting) password.length >= 10 && confirm == password else password.isNotEmpty(), onClick = {
        val target = uri!!; val secret = password.toCharArray(); uri = null; password = ""; confirm = ""
        if (exporting) vm.export(target, secret) else vm.preview(target, secret)
    }) { Text(if (exporting) "Export" else "Preview") } }, dismissButton = { TextButton(onClick = { uri = null; password = ""; confirm = "" }) { Text(stringResource(R.string.ui_cancel)) } })
    state.preview?.let { preview -> Confirm("Replace all local data?", "This backup contains ${preview.periods.size} periods and ${preview.logs.size} daily logs. Restoring replaces all current records and preferences.",
        "Replace data", vm::dismiss, vm::restore) }
}
