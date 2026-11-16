package com.periodtracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.periodtracker.domain.Period
import com.periodtracker.domain.PeriodStatus
import com.periodtracker.ui.period.PeriodDraft
import java.time.LocalDate
import java.util.UUID

class PeriodEditorViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val values = saved.getStateFlow("draft", emptyList<String>())
    private val initial get() = requireNotNull(saved.get<List<String>>("initial"))
    val id: String get() = requireNotNull(saved["id"])
    fun initialize(existing: Period?, date: LocalDate, finish: Boolean, today: LocalDate) {
        if (values.value.isNotEmpty()) return
        val draft = PeriodDraft.initial(existing, date, finish, today)
        val fields = listOf(draft.start, draft.end, draft.status.name)
        saved["id"] = existing?.id ?: UUID.randomUUID().toString()
        saved["initial"] = fields
        saved["draft"] = fields
    }
    val draft get() = values.value.let { PeriodDraft(it[0], it[1], PeriodStatus.valueOf(it[2])) }
    val changed get() = values.value != initial
    fun start(value: String) = update(0, value)
    fun end(value: String) = update(1, value)
    fun status(value: PeriodStatus) = update(2, value.name)
    private fun update(index: Int, value: String) { saved["draft"] = values.value.toMutableList().apply { this[index] = value } }
}
