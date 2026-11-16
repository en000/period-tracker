package com.periodtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.periodtracker.R
import com.periodtracker.domain.*
import com.periodtracker.ui.calendar.CalendarView
import com.periodtracker.ui.daydetails.DayDetailsView
import com.periodtracker.ui.insights.InsightsView
import com.periodtracker.ui.onboarding.OnboardingView
import com.periodtracker.ui.period.PeriodEditor
import com.periodtracker.ui.settings.SettingsView
import com.periodtracker.ui.theme.PeriodTrackerTheme
import com.periodtracker.ui.viewmodel.*
import java.time.LocalDate
import java.time.YearMonth

@Composable fun JournalApp(vm: JournalViewModel, backup: BackupViewModel, notificationDate: String?, handled: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val data = state.journal
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LifecycleResumeEffect(Unit) { vm.refreshDate(); onPauseOrDispose {} }
    PeriodTrackerTheme(data?.preferences?.appearance ?: Appearance.SYSTEM) {
        Surface(Modifier.fillMaxSize()) {
            if (data == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.error == null) CircularProgressIndicator()
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.load_failed))
                    Button(onClick = vm::retryLoading) { Text(stringResource(R.string.retry)) }
                }
            }
            else if (!data.preferences.onboardingComplete) Scaffold { padding ->
                Box(Modifier.padding(padding)) { OnboardingView(data.preferences, state.busy, vm) }
            }
            else {
                val navigation = rememberNavController()
                val entry by navigation.currentBackStackEntryAsState()
                val tabs = listOf("calendar", "insights", "settings")
                val labels = listOf(stringResource(R.string.calendar), stringResource(R.string.insights), stringResource(R.string.settings))
                val icons = listOf(Icons.Outlined.CalendarMonth, Icons.Outlined.Insights, Icons.Outlined.Settings)
                var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }
                var editId by rememberSaveable { mutableStateOf<String?>(null) }
                var editDate by rememberSaveable { mutableStateOf<String?>(null) }
                var finish by rememberSaveable { mutableStateOf(false) }
                val calendar: CalendarViewModel = viewModel()
                val month by calendar.month.collectAsStateWithLifecycle()
                fun editor(period: Period?, date: LocalDate, ending: Boolean = false) {
                    selectedDay = null; editId = period?.id; editDate = date.toString(); finish = ending
                }
                LaunchedEffect(notificationDate) { if (notificationDate != null) { navigation.navigate("calendar") { launchSingleTop = true }; selectedDay = notificationDate; handled() } }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                    NavigationBar { tabs.forEachIndexed { index, route ->
                        NavigationBarItem(entry?.destination?.route == route, onClick = {
                            navigation.navigate(route) { popUpTo(navigation.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                        }, icon = { Icon(icons[index], null) }, label = { Text(labels[index]) })
                    } }
                }) { padding ->
                    Box(Modifier.padding(padding)) {
                        NavHost(navigation, "calendar") {
                            composable("calendar") { CalendarView(data, state.today, YearMonth.parse(month), { calendar.move(-1) }, { calendar.move(1) }, calendar::today,
                                { selectedDay = it.toString() }, { val active = data.periods.find { it.status == PeriodStatus.ONGOING }; editor(active, state.today, active != null) }) }
                            composable("insights") { InsightsView(data, { editor(it, it.start) }, { selectedDay = it.toString() }) }
                            composable("settings") { SettingsView(data.preferences, state.busy, vm, backup) }
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
                selectedDay?.let { day -> key(day) { DayDetailsView(LocalDate.parse(day), data, state.busy, vm, { selectedDay = null }, { period, ending -> editor(period, LocalDate.parse(day), ending) }) } }
                editDate?.let { date -> key(editId, date, finish) { PeriodEditor(data.periods.find { it.id == editId }, LocalDate.parse(date), finish, data, state.busy, vm, { editDate = null; editId = null }) } }
            }
            state.error?.takeIf { data != null }?.let { error -> AlertDialog(onDismissRequest = vm::clearError, title = { Text(stringResource(R.string.ui_unable_to_complete)) }, text = { Text(error) },
                confirmButton = { TextButton(onClick = vm::clearError) { Text(stringResource(R.string.ui_ok)) } }) }
        }
    }
}
