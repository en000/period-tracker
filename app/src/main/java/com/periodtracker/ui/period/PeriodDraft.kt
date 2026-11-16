package com.periodtracker.ui.period

import com.periodtracker.domain.*
import java.time.LocalDate

/** Editable values are distinct from a validated record; opening a form never mutates data. */
data class PeriodDraft(val start: String, val end: String, val status: PeriodStatus) {
    fun validate(id: String, periods: List<Period>, today: LocalDate): PeriodDraftResult {
        val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
            ?: return PeriodDraftResult.Invalid("Choose a valid start date.")
        val endDate = if (status == PeriodStatus.COMPLETED) {
            runCatching { LocalDate.parse(end) }.getOrNull()
                ?: return PeriodDraftResult.Invalid("Choose the actual last bleeding day.")
        } else null
        val candidate = Period(id, startDate, endDate, status)
        val all = periods.filterNot { it.id == id } + candidate
        try { PeriodValidation.validate(all, today) }
        catch (e: IllegalArgumentException) { return PeriodDraftResult.Invalid(requireNotNull(e.message)) }
        val unusual = all.sortedBy { it.start }.zipWithNext().any { (a, b) ->
            (a.id == id || b.id == id) && daysBetween(a.start, b.start) !in 15L..90L
        }
        return PeriodDraftResult.Valid(candidate, unusual)
    }

    companion object {
        fun initial(existing: Period?, date: LocalDate, finish: Boolean, today: LocalDate) = PeriodDraft(
            (existing?.start ?: date).toString(),
            if (finish) date.toString() else existing?.end?.toString().orEmpty(),
            if (finish) PeriodStatus.COMPLETED else existing?.status
                ?: if (date == today) PeriodStatus.ONGOING else PeriodStatus.END_UNKNOWN,
        )
    }
}

sealed interface PeriodDraftResult {
    data class Invalid(val message: String) : PeriodDraftResult
    data class Valid(val period: Period, val requiresConfirmation: Boolean) : PeriodDraftResult
}
