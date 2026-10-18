package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryWeightSeriesTest {
    @Test
    fun usesSavedWeightsAcrossProgramsInTimestampThenIdOrder() {
        val newest = record(3, 300, 82.5).copy(workout = CompletedWorkoutType.B)
        val older = record(1, 100, 75.0)
        val tied = record(2, 300, 80.0)
        val records = listOf(newest, older, tied)
        val points = records.weightHistory(CompletedExerciseType.SQUAT)

        assertEquals(listOf(1L, 2L, 3L), points.map { it.workoutId })
        assertEquals(listOf(75.0, 80.0, 82.5), points.map { it.weightKg })
        assertEquals(listOf(300L, 100L, 300L), records.map { it.completedAtEpochMillis })
    }

    @Test
    fun includesPartialRepsAndEditedNamesButExcludesSkippedOrOtherLifts() {
        val partial = record(1, 100, 37.5).let { record ->
            record.copy(exercises = record.exercises.map { exercise ->
                exercise.copy(customName = "Paused squat", reps = 8, setStates = listOf(
                    CompletedWorkoutSet(0, true, 1), CompletedWorkoutSet(1, false),
                ))
            })
        }
        val skipped = record(2, 200, 100.0, completed = false)
        val otherLift = record(3, 300, 60.0, exercise = CompletedExerciseType.BENCH_PRESS)

        assertEquals(listOf(37.5), listOf(partial, skipped, otherLift)
            .weightHistory(CompletedExerciseType.SQUAT).map { it.weightKg })
        assertEquals(listOf(60.0), listOf(partial, skipped, otherLift)
            .weightHistory(CompletedExerciseType.BENCH_PRESS).map { it.weightKg })
        assertTrue(listOf(partial, skipped).weightHistory(CompletedExerciseType.DEADLIFT).isEmpty())
        assertTrue(emptyList<CompletedWorkoutRecord>().weightHistory(CompletedExerciseType.SQUAT).isEmpty())
    }

    @Test
    fun timeAxisUsesElapsedTimeAndHandlesSingleAndTiedTimestamps() {
        val points = listOf(record(1, 100), record(2, 200), record(3, 500))
            .weightHistory(CompletedExerciseType.SQUAT)
        assertEquals(0f, points.timeFraction(0))
        assertEquals(0.25f, points.timeFraction(1))
        assertEquals(1f, points.timeFraction(2))
        assertEquals(0.5f, points.take(1).timeFraction(0))
        val tied = listOf(record(1, 100), record(2, 100)).weightHistory(CompletedExerciseType.SQUAT)
        assertEquals(0.5f, tied.timeFraction(0))
        assertEquals(0.5f, tied.timeFraction(1))
    }

    @Test
    fun preservesFlatAndZeroWeightsAndRejectsInvalidWeights() {
        val points = listOf(record(1, 100, 0.0), record(2, 200, 0.0),
            record(3, 300, Double.NaN), record(4, 400, Double.POSITIVE_INFINITY), record(5, 500, -1.0))
            .weightHistory(CompletedExerciseType.SQUAT)
        assertEquals(listOf(0.0, 0.0), points.map { it.weightKg })
    }

    private fun record(
        id: Long,
        timestamp: Long,
        weight: Double = 20.0,
        completed: Boolean = true,
        exercise: CompletedExerciseType = CompletedExerciseType.SQUAT,
    ) = CompletedWorkoutRecord(
        id, CompletedWorkoutType.A, timestamp - 60_000, timestamp,
        listOf(CompletedWorkoutExercise(exercise, 1, 5, weight, listOf(CompletedWorkoutSet(0, completed)))),
    )
}
