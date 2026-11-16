package com.periodtracker.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.periodtracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
fun enumLabel(value: Enum<*>): String = value.name.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) }
@Composable fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        content()
        Spacer(Modifier.height(24.dp))
    }
}
@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge); content()
        }
    }
}
@Composable fun Action(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) { Text(label) }
}
@Composable fun DateField(label: String, value: String, onChange: (String) -> Unit, optional: Boolean = false) {
    val context = LocalContext.current
    Column {
        OutlinedButton(onClick = {
            val initial = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
            DatePickerDialog(context, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d).toString()) },
                initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
                datePicker.maxDate = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }.show()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("$label: ${value.takeIf { it.isNotBlank() }?.let { dateLabel(LocalDate.parse(it)) } ?: "Not set"}")
        }
        if (optional && value.isNotBlank()) TextButton(onClick = { onChange("") }) { Text(stringResource(R.string.ui_clear_date)) }
    }
}
@Composable fun <T> Choices(label: String, items: List<T>, selected: (T) -> Boolean, text: (T) -> String, onClick: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item -> FilterChip(selected(item), { onClick(item) }, label = { Text(text(item)) }, modifier = Modifier.heightIn(min = 48.dp)) }
        }
    }
}
@Composable fun Confirm(title: String, body: String, confirmLabel: String = "Confirm", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } })
}
