package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HistoryFormattingTest {
    @Test
    fun recordedRepsDoNotInventUnavailableOrUnrecordedValues() {
        val exercise = CompletedWorkoutExercise(
            CompletedExerciseType.SQUAT, 5, 8, 80.0,
            listOf(CompletedWorkoutSet(0, true, 8), CompletedWorkoutSet(1, true, 3),
                CompletedWorkoutSet(2, true, 1), CompletedWorkoutSet(3, false), CompletedWorkoutSet(4, true)),
        )
        assertEquals("Reps: 8 / 3 / 1 / – / ?", exercise.recordedRepsLabel())
        assertEquals(4, exercise.completedSets)
    }

    @Test
    fun datesUseTheSavedUtcCalendarDate() {
        val newYear = record(completedAt = 0L)
        val leapDay = record(completedAt = 1_582_934_400_000L)

        assertEquals("Jan 1", newYear.dateLabel())
        assertEquals("January 1, 1970", newYear.detailDateLabel())
        assertEquals("Feb 29", leapDay.dateLabel())
        assertEquals("February 29, 2020", leapDay.detailDateLabel())
    }

    @Test
    fun durationsUseCompactListAndClockLabels() {
        val workout = record(
            startedAt = 0L,
            completedAt = 3_661_000L,
        )

        assertEquals("1 h 1 min", workout.durationLabel())
        assertEquals("1:01:01", workout.durationClockLabel())
    }

    @Test
    fun dateTilesUseRealWeekdaysIncludingLeapDaysAndBeforeEpoch() {
        val epoch = record(completedAt = 0L)
        val beforeEpoch = record(completedAt = -1L)
        val leapDay = record(completedAt = 1_582_934_400_000L)

        assertEquals("JANUARY 1970", epoch.monthLabel())
        assertEquals("THU", epoch.weekdayLabel())
        assertEquals("1", epoch.dayLabel())
        assertEquals("DECEMBER 1969", beforeEpoch.monthLabel())
        assertEquals("WED", beforeEpoch.weekdayLabel())
        assertEquals("31", beforeEpoch.dayLabel())
        assertEquals("FEBRUARY 2020", leapDay.monthLabel())
        assertEquals("SAT", leapDay.weekdayLabel())
        assertEquals("29", leapDay.dayLabel())
    }

    @Test
    fun monthGroupsIncludeYearAndPreserveSourceOrderForTiedDates() {
        val january = record(completedAt = 1_767_225_600_000L).copy(id = 4L)
        val tiedJanuary = january.copy(id = 3L)
        val december = record(completedAt = 1_767_225_599_999L).copy(id = 2L)
        val previousJanuary = record(completedAt = 1_735_689_600_000L).copy(id = 1L)
        val months = listOf(january, tiedJanuary, december, previousJanuary).historyMonths()

        assertEquals(listOf("JANUARY 2026", "DECEMBER 2025", "JANUARY 2025"), months.keys.toList())
        assertEquals(listOf(4L, 3L), months.getValue("JANUARY 2026").map { it.id })
        assertEquals(listOf(2L), months.getValue("DECEMBER 2025").map { it.id })
        assertEquals(emptyMap(), emptyList<CompletedWorkoutRecord>().historyMonths())
    }

    @Test
    fun countsUseSavedPrescriptionsEvenWhenTheSessionIsPartialOrEdited() {
        val partialB = record(completedAt = 0L).copy(
            workout = CompletedWorkoutType.B,
            exercises = listOf(
                exercise(CompletedExerciseType.SQUAT, 5),
                exercise(CompletedExerciseType.OVERHEAD_PRESS, 5),
                exercise(CompletedExerciseType.DEADLIFT, 1),
            ),
        )
        val edited = partialB.copy(exercises = listOf(exercise(CompletedExerciseType.DEADLIFT, 2)))
        val singleSet = partialB.copy(exercises = listOf(exercise(CompletedExerciseType.DEADLIFT, 1)))

        assertEquals(0, partialB.completedSets)
        assertEquals("3 exercises  ·  11 sets", partialB.sessionSummary())
        assertEquals("1 exercise  ·  2 sets", edited.sessionSummary())
        assertEquals("1 exercise  ·  1 set", singleSet.sessionSummary())
    }

    private fun exercise(type: CompletedExerciseType, sets: Int) = CompletedWorkoutExercise(
        exercise = type,
        sets = sets,
        reps = 5,
        weightKg = 20.0,
        setStates = List(sets) { CompletedWorkoutSet(it, false) },
    )

    private fun record(
        startedAt: Long = 0L,
        completedAt: Long,
    ) = CompletedWorkoutRecord(
        id = 1L,
        workout = CompletedWorkoutType.A,
        startedAtEpochMillis = startedAt,
        completedAtEpochMillis = completedAt,
        exercises = emptyList(),
    )
}
