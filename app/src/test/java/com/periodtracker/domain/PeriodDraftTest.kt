package com.periodtracker.domain

import com.periodtracker.ui.period.*
import com.periodtracker.ui.viewmodel.backupFailureMessage
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import javax.crypto.AEADBadTagException

class PeriodDraftTest {
    private val today = LocalDate.of(2026, 11, 16)
    @Test fun initialTodayIsOngoingAndHistoricalStartIsUnknown() {
        assertEquals(PeriodStatus.ONGOING, PeriodDraft.initial(null, today, false, today).status)
        assertEquals(PeriodStatus.END_UNKNOWN, PeriodDraft.initial(null, today.minusDays(2), false, today).status)
    }
    @Test fun endingUsesSelectedInclusiveDateAndKeepsStart() {
        val existing = Period("a", today.minusDays(4), status = PeriodStatus.ONGOING)
        val draft = PeriodDraft.initial(existing, today, true, today)
        val result = draft.validate("a", listOf(existing), today) as PeriodDraftResult.Valid
        assertEquals(5L, result.period.completedDuration)
    }
    @Test fun completedWithoutEndGivesSpecificValidationError() {
        val result = PeriodDraft(today.toString(), "", PeriodStatus.COMPLETED).validate("a", emptyList(), today)
        assertEquals(PeriodDraftResult.Invalid("Choose the actual last bleeding day."), result)
    }
    @Test fun unusualIntervalRequiresConfirmationButOverlapsAreRejected() {
        val existing = Period("a", today.minusDays(5), today.minusDays(2), PeriodStatus.COMPLETED)
        val draft = PeriodDraft.initial(null, today, false, today)
        assertTrue((draft.validate("b", listOf(existing), today) as PeriodDraftResult.Valid).requiresConfirmation)
        assertTrue(draft.copy(start = today.minusDays(3).toString()).validate("b", listOf(existing), today) is PeriodDraftResult.Invalid)
    }
    @Test fun backupErrorsDoNotLeakProviderOrHealthDetails() {
        assertTrue(backupFailureMessage(AEADBadTagException()).contains("passphrase"))
        assertFalse(backupFailureMessage(SecurityException("private-uri")).contains("private-uri"))
    }
}
