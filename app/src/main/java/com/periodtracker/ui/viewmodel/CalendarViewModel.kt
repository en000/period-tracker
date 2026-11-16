package com.periodtracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.time.YearMonth

class CalendarViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val month = savedState.getStateFlow("month", YearMonth.now().toString())
    fun move(delta: Long) { savedState["month"] = YearMonth.parse(month.value).plusMonths(delta).toString() }
    fun today() { savedState["month"] = YearMonth.now().toString() }
}
