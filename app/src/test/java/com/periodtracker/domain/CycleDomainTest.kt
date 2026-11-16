package com.periodtracker.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CycleDomainTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
    private val engine = CyclePredictionEngine(clock)
    private val defaults = CyclePreferences()
    private fun period(start: String, end: String? = null, ongoing: Boolean = false): Period = Period(
        start, LocalDate.parse(start), end?.let(LocalDate::parse),
        if (ongoing) PeriodStatus.ONGOING else if (end != null) PeriodStatus.COMPLETED else PeriodStatus.END_UNKNOWN,
    )
    private fun rejected(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun noHistoryProducesNoEstimateOrInventedStatistics() {
        assertNull(engine.predict(emptyList(), defaults))
        assertEquals(CycleStatus.NoHistory, engine.status(emptyList(), defaults))
        val stats = CycleStatisticsEngine.calculate(emptyList())
        assertNull(stats.typicalCycle)
        assertNull(stats.typicalPeriod)
    }

    @Test fun usualLengthFallbackMatchesSpecification() {
        val result = engine.predict(listOf(period("2026-11-01")), defaults)!!
        assertEquals(LocalDate.parse("2026-11-29"), result.centralStart)
        assertEquals(LocalDate.parse("2026-11-26"), result.earliestStart)
        assertEquals(LocalDate.parse("2026-12-02"), result.latestStart)
        assertEquals(EstimateBasis.USUAL_LENGTH, result.basis)
        assertEquals(5L, result.duration)
    }

    @Test fun oneIntervalBlendsWithPreferenceUsingHalfUpRounding() {
        val result = engine.predict(listOf(period("2026-10-01"), period("2026-11-01")), defaults)!!
        assertEquals(30L, result.cycleLength)
        assertEquals(EstimateBasis.PRELIMINARY, result.basis)
        assertEquals(LocalDate.parse("2026-12-01"), result.centralStart)
        assertEquals(LocalDate.parse("2026-11-28"), result.earliestStart)
    }

    @Test fun observedMedianMatchesSpecificationIncludingNonChronologicalInput() {
        val data = listOf("2025-03-27", "2025-01-01", "2025-02-28", "2025-01-29").map { period(it) }
        val result = engine.predict(data, defaults)!!
        assertEquals(listOf(28L, 30L, 27L), observedIntervals(data))
        assertEquals(28L, result.cycleLength)
        assertEquals(LocalDate.parse("2025-04-24"), result.centralStart)
    }

    @Test fun evenMedianAndMidpointRoundHalfUpInBothDirections() {
        assertEquals(29L, median(listOf(28L, 29L)))
        assertEquals(27L, halfUpMidpoint(28, 25))
        assertEquals(27L, halfUpMidpoint(25, 28))
        assertEquals(28L, median(listOf(1L, 27L, 28L, 100L)))
    }

    @Test fun onlySixLatestIntervalsInfluencePrediction() {
        val data = listOf(0L, 90L, 118L, 146L, 174L, 202L, 230L, 258L)
            .map { period(LocalDate.parse("2025-01-01").plusDays(it).toString()) }
        val result = engine.predict(data, defaults)!!
        assertEquals(28L, result.cycleLength)
        assertFalse(result.timingVaries)
    }

    @Test fun variableIntervalsWidenRangeWithoutFiltering() {
        val data = listOf("2026-01-01", "2026-01-21", "2026-04-21").map { period(it) }
        val result = engine.predict(data, defaults)!!
        assertEquals(55L, result.cycleLength)
        assertTrue(result.timingVaries)
        assertEquals(LocalDate.parse("2026-05-11"), result.earliestStart)
        assertEquals(LocalDate.parse("2026-07-20"), result.latestStart)
    }

    @Test fun pastEstimateIsRetainedAndOngoingOverridesIt() {
        val start = period("2026-10-01")
        val status = engine.status(listOf(start), defaults)
        assertTrue(status is CycleStatus.PreviousEstimate)
        assertEquals(LocalDate.parse("2026-10-29"), (status as CycleStatus.PreviousEstimate).prediction.centralStart)
        assertEquals(CycleStatus.Ongoing(47, true), engine.status(listOf(start.copy(status = PeriodStatus.ONGOING)), defaults))
    }

    @Test fun centralEstimateTodayAndFirstPeriodDayAreDistinct() {
        assertTrue(engine.status(listOf(period("2026-10-19")), defaults) is CycleStatus.AroundToday)
        assertEquals(CycleStatus.Ongoing(1, false), engine.status(listOf(period("2026-11-16", ongoing = true)), defaults))
    }

    @Test fun periodDurationIsInclusiveAcrossLeapDayAndMonthBoundary() {
        val completed = period("2024-02-28", "2024-03-02")
        assertEquals(4L, completed.completedDuration)
        assertTrue(completed.contains(LocalDate.parse("2024-02-29"), today))
        assertEquals(3L, cycleDay(listOf(completed), LocalDate.parse("2024-03-01")))
    }

    @Test fun unknownEndMarksOnlyKnownStartAndDoesNotContributeDuration() {
        val unknown = period("2026-11-01")
        assertTrue(unknown.contains(unknown.start, today))
        assertFalse(unknown.contains(unknown.start.plusDays(1), today))
        assertNull(unknown.completedDuration)
        assertNull(CycleStatisticsEngine.calculate(listOf(unknown)).typicalPeriod)
    }

    @Test fun completedDurationMedianIgnoresOngoingAndUnknown() {
        val data = listOf(period("2026-08-01", "2026-08-04"), period("2026-09-01", "2026-09-05"),
            period("2026-10-01"), period("2026-11-01", ongoing = true))
        assertEquals(5L, engine.predict(data, defaults)!!.duration)
    }

    @Test fun invalidAndOverlappingPeriodsAreRejected() {
        rejected { PeriodValidation.validate(listOf(period("2026-12-01")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-11-01", "2026-10-30")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-11-01", "2026-12-01")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-10-01", "2026-10-05"), period("2026-10-05")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-10-01"), period("2026-10-01").copy(id = "other")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-10-01", ongoing = true), period("2026-11-01")), today) }
        rejected { PeriodValidation.validate(listOf(period("2026-10-01", ongoing = true), period("2026-11-01", ongoing = true)), today) }
    }

    @Test fun backfillAllowsUnknownEndAndUnusualButNonoverlappingIntervals() {
        val data = listOf(period("2026-11-01"), period("2026-10-03", "2026-10-04"), period("2026-10-01"))
        PeriodValidation.validate(data, today)
        assertEquals(listOf(2L), PeriodValidation.unusualIntervals(data))
    }

    @Test fun travelDoesNotMoveRecordedDatesOrPredictions() {
        val data = listOf(period("2026-11-01"))
        val otherZone = CyclePredictionEngine(clock.withZone(ZoneId.of("Pacific/Auckland")))
        assertEquals(engine.predict(data, defaults), otherZone.predict(data, defaults))
        assertEquals(LocalDate.parse("2026-11-01"), data.single().start)
    }

    @Test fun dailyLogDistinguishesUnspecifiedFromExplicitNoneAndValidatesSymptoms() {
        assertTrue(DailyLog(today).isEmpty)
        assertFalse(DailyLog(today, flow = FlowLevel.NONE).isEmpty)
        assertFalse(DailyLog(today, noSymptomsExplicit = true).isEmpty)
        DailyLog(today, symptoms = setOf(Symptom.CRAMPS), noSymptomsExplicit = false).validate(today)
        rejected { DailyLog(today.plusDays(1), flow = FlowLevel.LIGHT).validate(today) }
        rejected { DailyLog(today, notes = "a".repeat(2_001)).validate(today) }
        rejected { DailyLog(today, symptoms = setOf(Symptom.CRAMPS), noSymptomsExplicit = true).validate(today) }
    }
}
