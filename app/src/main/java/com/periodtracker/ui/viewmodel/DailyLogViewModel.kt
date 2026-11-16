package com.periodtracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.periodtracker.domain.*
import java.time.LocalDate

/** Owns a date-specific draft; saved primitive values survive activity/process recreation. */
class DailyLogViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val fields = saved.getStateFlow("fields", emptyList<String>())
    val symptoms = saved.getStateFlow("symptoms", emptyList<String>())
    val noSymptoms = saved.getStateFlow("noSymptoms", false)

    fun initialize(date: LocalDate, existing: DailyLog?) {
        if (fields.value.isNotEmpty()) return
        val log = existing ?: DailyLog(date)
        saved["date"] = date.toString()
        val values = listOf(log.flow?.name.orEmpty(), log.mood?.name.orEmpty(), log.pain?.name.orEmpty(), log.notes)
        saved["initialFields"] = values
        saved["initialSymptoms"] = log.symptoms.map { it.name }
        saved["initialNone"] = log.noSymptomsExplicit == true
        saved["fields"] = values
        saved["symptoms"] = log.symptoms.map { it.name }
        saved["noSymptoms"] = log.noSymptomsExplicit == true
    }
    val changed: Boolean get() = fields.value != saved.get<List<String>>("initialFields") ||
        symptoms.value.toSet() != saved.get<List<String>>("initialSymptoms")?.toSet() ||
        noSymptoms.value != saved.get<Boolean>("initialNone")

    fun field(index: Int, value: String) {
        if (index == 3 && value.length > 2000) return
        saved["fields"] = fields.value.toMutableList().apply { this[index] = value }
    }
    fun toggleSymptom(symptom: Symptom) {
        saved["noSymptoms"] = false
        saved["symptoms"] = if (symptom.name in symptoms.value) symptoms.value - symptom.name else symptoms.value + symptom.name
    }
    fun toggleNone() {
        saved["noSymptoms"] = !noSymptoms.value
        if (noSymptoms.value) saved["symptoms"] = emptyList<String>()
    }
    fun draft(): DailyLog = DailyLog(
        LocalDate.parse(requireNotNull(saved["date"])),
        fields.value[0].takeIf(String::isNotBlank)?.let(FlowLevel::valueOf),
        fields.value[1].takeIf(String::isNotBlank)?.let(Mood::valueOf),
        fields.value[2].takeIf(String::isNotBlank)?.let(Pain::valueOf), fields.value[3],
        symptoms.value.map(Symptom::valueOf).toSet(),
        if (noSymptoms.value) true else if (symptoms.value.isNotEmpty()) false else null,
    )
}
