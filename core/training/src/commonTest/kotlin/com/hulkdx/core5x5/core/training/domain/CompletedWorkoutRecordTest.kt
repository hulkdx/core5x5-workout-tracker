package com.hulkdx.core5x5.core.training.domain

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CompletedWorkoutRecordTest {
    @Test
    fun durationAndSetTotalsUseTheSavedSnapshot() {
        val record = CompletedWorkoutRecord(
            id = 7L,
            workout = CompletedWorkoutType.B,
            startedAtEpochMillis = 5_000L,
            completedAtEpochMillis = 2_000L,
            exercises = listOf(
                CompletedWorkoutExercise(
                    exercise = CompletedExerciseType.SQUAT,
                    sets = 5,
                    reps = 5,
                    weightKg = 42.5,
                    setStates = List(5) { CompletedWorkoutSet(it, it < 2) },
                ),
                CompletedWorkoutExercise(
                    exercise = CompletedExerciseType.DEADLIFT,
                    sets = 1,
                    reps = 5,
                    weightKg = 60.0,
                    setStates = listOf(CompletedWorkoutSet(0, true)),
                ),
            ),
        )

        assertEquals(0L, record.durationMillis)
        assertEquals(6, record.prescribedSets)
        assertEquals(3, record.completedSets)
    }
}
