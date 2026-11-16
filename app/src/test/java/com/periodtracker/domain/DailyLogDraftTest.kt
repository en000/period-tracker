package com.periodtracker.domain

import androidx.lifecycle.SavedStateHandle
import com.periodtracker.ui.viewmodel.DailyLogViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DailyLogDraftTest {
    private val today = LocalDate.of(2026, 11, 16)
    @Test fun changingOneFieldPreservesOtherObservations() {
        val existing = DailyLog(today, flow = FlowLevel.LIGHT, mood = Mood.GOOD, notes = "Existing note")
        val model = DailyLogViewModel(SavedStateHandle())
        model.initialize(today, existing)
        assertFalse(model.changed)
        model.field(2, Pain.MILD.name)
        assertEquals(existing.copy(pain = Pain.MILD), model.draft())
        assertTrue(model.changed)
    }
    @Test fun symptomAndExplicitNoneRemainMutuallyExclusive() {
        val model = DailyLogViewModel(SavedStateHandle())
        model.initialize(today, null)
        model.toggleSymptom(Symptom.CRAMPS)
        model.toggleNone()
        assertTrue(model.draft().symptoms.isEmpty())
        assertEquals(true, model.draft().noSymptomsExplicit)
        model.toggleSymptom(Symptom.FATIGUE)
        assertEquals(false, model.draft().noSymptomsExplicit)
        model.draft().validate(today)
    }
    @Test fun restorationDoesNotOverwriteDraftWithDatabaseValues() {
        val saved = SavedStateHandle()
        val first = DailyLogViewModel(saved)
        first.initialize(today, null)
        first.field(3, "Unsaved note")
        val recreated = DailyLogViewModel(saved)
        recreated.initialize(today, null)
        assertEquals("Unsaved note", recreated.draft().notes)
        assertTrue(recreated.changed)
    }
}
