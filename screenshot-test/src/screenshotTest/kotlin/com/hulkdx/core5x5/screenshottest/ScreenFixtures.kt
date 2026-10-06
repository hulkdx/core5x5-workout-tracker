package com.hulkdx.core5x5.screenshottest

import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteUiState

internal fun unfinishedWorkout(workout: Workout) = UnfinishedWorkout(
    workout = workout,
    startedAtEpochMillis = 0L,
    exercises = workout.exerciseSnapshots(completedSets = 2),
    id = 1L,
)

internal fun completedWorkoutState(
    workout: Workout,
    completedSets: Int = workout.exercises.sumOf { it.sets },
) = WorkoutCompleteUiState(
    isLoading = false,
    completedWorkout = CompletedWorkout(
        id = 1L,
        workout = workout,
        startedAtEpochMillis = 0L,
        completedAtEpochMillis = 2_538_000L,
        exercises = workout.exerciseSnapshots(completedSets),
    ),
    nextWorkout = WorkoutPrescription(workout.nextWorkout()),
)

private fun Workout.exerciseSnapshots(completedSets: Int): List<UnfinishedWorkoutExercise> {
    var remainingCompleted = completedSets
    return exercises.map { exercise ->
        UnfinishedWorkoutExercise(
            exercise = exercise,
            sets = exercise.sets,
            reps = exercise.reps,
            weightKg = exercise.startingWeightKg,
            setStates = List(exercise.sets) { position ->
                UnfinishedWorkoutSet(position, isCompleted = remainingCompleted-- > 0)
            },
        )
    }
}
