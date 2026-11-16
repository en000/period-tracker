package com.periodtracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.periodtracker.domain.*
import com.periodtracker.ui.calendar.CalendarView
import com.periodtracker.ui.theme.PeriodTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun emptyCalendarExplainsMissingHistoryAndDateSelectionWorks() {
        val today = LocalDate.of(2026, 11, 16)
        var selected: LocalDate? = null
        compose.setContent {
            PeriodTrackerTheme { CalendarView(JournalSnapshot(), today, YearMonth.from(today), {}, {}, {}, { selected = it }, {}) }
        }
        compose.onNodeWithText("Add your first period to see an estimate").assertExists()
        compose.onNodeWithContentDescription("Today", substring = true).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(today, selected) }
    }
    @Test fun ongoingPeriodUsesEndActionAndIncludesAccessibleRecordedDates() {
        val today = LocalDate.of(2026, 11, 16)
        val data = JournalSnapshot(periods = listOf(Period("one", today, status = PeriodStatus.ONGOING)))
        compose.setContent { PeriodTrackerTheme { CalendarView(data, today, YearMonth.from(today), {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Period day 1").assertExists()
        compose.onNodeWithText("End Period").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Recorded period", substring = true).assertExists()
    }
}
