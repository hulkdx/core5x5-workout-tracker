package com.hulkdx.core5x5.feature.workout.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutProgramTest {
    @Test
    fun completingWorkoutASelectsWorkoutB() {
        assertEquals(Workout.B, Workout.A.nextWorkout())
    }

    @Test
    fun completingWorkoutBSelectsWorkoutA() {
        assertEquals(Workout.A, Workout.B.nextWorkout())
    }

    @Test
    fun workoutAContainsItsThreeExercisesInTrainingOrder() {
        assertEquals(
            listOf(Exercise.SQUAT, Exercise.BENCH_PRESS, Exercise.BARBELL_ROW),
            Workout.A.exercises,
        )
    }

    @Test
    fun workoutBContainsItsThreeExercisesInTrainingOrder() {
        assertEquals(
            listOf(Exercise.SQUAT, Exercise.OVERHEAD_PRESS, Exercise.DEADLIFT),
            Workout.B.exercises,
        )
    }

    @Test
    fun programContainsOnlyTheFiveExercisesAndTwoWorkouts() {
        assertEquals(listOf(Workout.A, Workout.B), Workout.entries)
        assertEquals(
            setOf(
                Exercise.SQUAT,
                Exercise.BENCH_PRESS,
                Exercise.BARBELL_ROW,
                Exercise.OVERHEAD_PRESS,
                Exercise.DEADLIFT,
            ),
            Exercise.entries.toSet(),
        )
    }

    @Test
    fun everyWorkoutUsesFiveSetsOfFiveExceptDeadlift() {
        for (workout in Workout.entries) {
            for (exercise in workout.exercises) {
                val expectedSets = if (exercise == Exercise.DEADLIFT) 1 else 5
                assertEquals(expectedSets, exercise.sets, "$workout $exercise sets")
                assertEquals(5, exercise.reps, "$workout $exercise reps")
            }
        }
    }

    @Test
    fun everyExerciseStartsAtTwentyKilograms() {
        for (exercise in Exercise.entries) {
            assertEquals(20.0, exercise.startingWeightKg, "$exercise starting weight in kg")
        }
    }
}
