package com.periodtracker.domain

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

enum class PeriodStatus { ONGOING, COMPLETED, END_UNKNOWN }

data class Period(
    val id: String,
    val start: LocalDate,
    val end: LocalDate? = null,
    val status: PeriodStatus = PeriodStatus.END_UNKNOWN,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val completedDuration: Long?
        get() = if (status == PeriodStatus.COMPLETED && end != null) daysBetween(start, end) + 1 else null

    fun contains(date: LocalDate, today: LocalDate): Boolean {
        val last = when (status) {
            PeriodStatus.COMPLETED -> end ?: start
            PeriodStatus.ONGOING -> today
            PeriodStatus.END_UNKNOWN -> start
        }
        return date >= start && date <= last
    }
}

enum class FlowLevel { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }
enum class Mood { VERY_GOOD, GOOD, NEUTRAL, LOW, VERY_LOW }
enum class Pain { NONE, MILD, MODERATE, SEVERE }
enum class Symptom { CRAMPS, BLOATING, HEADACHE, FATIGUE, BACK_PAIN, BREAST_TENDERNESS, ACNE, NAUSEA, OTHER }

data class DailyLog(
    val date: LocalDate,
    val flow: FlowLevel? = null,
    val mood: Mood? = null,
    val pain: Pain? = null,
    val notes: String = "",
    val symptoms: Set<Symptom> = emptySet(),
    val noSymptomsExplicit: Boolean? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val isEmpty: Boolean get() = flow == null && mood == null && pain == null &&
        notes.isBlank() && symptoms.isEmpty() && noSymptomsExplicit != true

    fun validate(today: LocalDate) {
        require(date <= today) { "Daily observations cannot be recorded in the future." }
        require(notes.length <= 2_000) { "Notes must be at most 2,000 characters." }
        require(noSymptomsExplicit != true || symptoms.isEmpty()) {
            "Choose symptoms or no symptoms, not both."
        }
        require(symptoms.isEmpty() || noSymptomsExplicit == false) {
            "Selected symptoms must have a matching symptom state."
        }
        require(noSymptomsExplicit != false || symptoms.isNotEmpty()) {
            "Select at least one symptom or leave symptoms unspecified."
        }
    }
}

data class CyclePreferences(val usualCycleLength: Int = 28, val usualPeriodDuration: Int = 5) {
    fun validate() {
        require(usualCycleLength in 15..90) { "Usual cycle length must be between 15 and 90 days." }
        require(usualPeriodDuration in 1..15) { "Usual period duration must be between 1 and 15 days." }
    }
}

object PeriodValidation {
    /** Validate the complete candidate dataset inside the repository's write transaction. */
    fun validate(periods: List<Period>, today: LocalDate) {
        require(periods.map { it.id }.distinct().size == periods.size) { "Period identifiers must be unique." }
        require(periods.all { it.id.isNotBlank() }) { "A period identifier is missing." }
        val sorted = periods.sortedBy { it.start }
        require(sorted.map { it.start }.distinct().size == sorted.size) { "A period already starts on this date." }
        require(sorted.count { it.status == PeriodStatus.ONGOING } <= 1) { "End the ongoing period first." }
        sorted.forEachIndexed { index, period ->
            require(period.start <= today) { "A period cannot start in the future." }
            when (period.status) {
                PeriodStatus.COMPLETED -> {
                    val end = requireNotNull(period.end) { "A completed period needs an end date." }
                    require(end >= period.start) { "End date must be on or after the start date." }
                    require(end <= today) { "A period cannot end in the future." }
                    sorted.getOrNull(index + 1)?.let { next ->
                        require(end < next.start) { "These period dates overlap another record." }
                    }
                }
                PeriodStatus.ONGOING -> {
                    require(period.end == null) { "An ongoing period cannot have an end date." }
                    require(index == sorted.lastIndex) { "Only the latest period can be ongoing." }
                }
                PeriodStatus.END_UNKNOWN -> require(period.end == null) {
                    "Choose completed when an end date is known."
                }
            }
        }
    }

    /** These intervals are allowed, but should receive an explanatory confirmation in the editor. */
    fun unusualIntervals(periods: List<Period>): List<Long> = observedIntervals(periods).filter { it !in 15L..90L }
}

enum class EstimateBasis { USUAL_LENGTH, PRELIMINARY, RECORDED_HISTORY }

data class Prediction(
    val centralStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val duration: Long,
    val cycleLength: Long,
    val basis: EstimateBasis,
    val timingVaries: Boolean,
) {
    val lastPredictedDay: LocalDate get() = centralStart.plusDays(duration - 1)
    fun contains(date: LocalDate): Boolean = date >= centralStart && date <= lastPredictedDay
}

sealed interface CycleStatus {
    data object NoHistory : CycleStatus
    data class Ongoing(val day: Long, val reviewSuggested: Boolean) : CycleStatus
    data class Upcoming(val days: Long, val prediction: Prediction) : CycleStatus
    data class AroundToday(val prediction: Prediction) : CycleStatus
    data class PreviousEstimate(val prediction: Prediction) : CycleStatus
}

class CyclePredictionEngine(private val clock: Clock = Clock.systemDefaultZone()) {
    companion object { const val ALGORITHM_VERSION = 1 }

    fun predict(periods: List<Period>, preferences: CyclePreferences): Prediction? {
        preferences.validate()
        val latest = periods.maxByOrNull { it.start } ?: return null
        val intervals = observedIntervals(periods).takeLast(6)
        val usual = preferences.usualCycleLength.toLong()
        val length = when (intervals.size) {
            0 -> usual
            1 -> halfUpMidpoint(usual, intervals.single())
            else -> requireNotNull(median(intervals))
        }
        val central = latest.start.plusDays(length)
        val preliminarySpread = if (intervals.size == 1) max(3, (abs(intervals.single() - usual) + 1) / 2) else 3
        val earliest = if (intervals.size < 2) central.minusDays(preliminarySpread)
            else latest.start.plusDays(minOf(intervals.min(), length - 2))
        val last = if (intervals.size < 2) central.plusDays(preliminarySpread)
            else latest.start.plusDays(maxOf(intervals.max(), length + 2))
        val duration = median(periods.sortedBy { it.start }.mapNotNull { it.completedDuration }.takeLast(6))
            ?: preferences.usualPeriodDuration.toLong()
        return Prediction(central, earliest, last, duration, length,
            when (intervals.size) {
                0 -> EstimateBasis.USUAL_LENGTH
                1 -> EstimateBasis.PRELIMINARY
                else -> EstimateBasis.RECORDED_HISTORY
            }, intervals.size >= 2 && intervals.max() - intervals.min() > 7)
    }

    fun status(periods: List<Period>, preferences: CyclePreferences): CycleStatus {
        val today = LocalDate.now(clock)
        periods.singleOrNull { it.status == PeriodStatus.ONGOING }?.let {
            val day = daysBetween(it.start, today) + 1
            return CycleStatus.Ongoing(day, day > 15)
        }
        val prediction = predict(periods, preferences) ?: return CycleStatus.NoHistory
        val days = daysBetween(today, prediction.centralStart)
        return when {
            days < 0 -> CycleStatus.PreviousEstimate(prediction)
            days == 0L -> CycleStatus.AroundToday(prediction)
            else -> CycleStatus.Upcoming(days, prediction)
        }
    }
}

data class CycleStatistics(
    val intervalCount: Int,
    val typicalCycle: Long?,
    val typicalPeriod: Long?,
    val shortestCycle: Long?,
    val longestCycle: Long?,
    val recentIntervals: List<Long>,
)

object CycleStatisticsEngine {
    fun calculate(periods: List<Period>): CycleStatistics {
        val intervals = observedIntervals(periods)
        return CycleStatistics(intervals.size, median(intervals),
            median(periods.mapNotNull { it.completedDuration }),
            intervals.minOrNull().takeIf { intervals.size >= 2 },
            intervals.maxOrNull().takeIf { intervals.size >= 2 }, intervals.takeLast(6))
    }
}

fun daysBetween(first: LocalDate, last: LocalDate): Long = ChronoUnit.DAYS.between(first, last)
fun observedIntervals(periods: List<Period>): List<Long> = periods.sortedBy { it.start }
    .zipWithNext { first, second -> daysBetween(first.start, second.start) }
fun cycleDay(periods: List<Period>, date: LocalDate): Long? = periods.filter { it.start <= date }
    .maxByOrNull { it.start }?.let { daysBetween(it.start, date) + 1 }

/** Positive date intervals use half-up rounding consistently, including even-sized medians. */
fun halfUpMidpoint(a: Long, b: Long): Long = a + (b - a) / 2 + if ((b - a) % 2 > 0) 1 else 0
fun median(values: List<Long>): Long? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else halfUpMidpoint(sorted[middle - 1], sorted[middle])
}
