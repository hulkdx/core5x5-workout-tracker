package com.hulkdx.core5x5.screenshottest

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
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

internal fun historyRecords() = listOf(
    CompletedWorkoutRecord(
        id = 4L,
        workout = CompletedWorkoutType.B,
        startedAtEpochMillis = 1_727_856_000_000L,
        completedAtEpochMillis = 1_727_858_460_000L,
        exercises = listOf(
            historyExercise(CompletedExerciseType.SQUAT, 77.5),
            historyExercise(CompletedExerciseType.DEADLIFT, 107.5, sets = 1),
            historyExercise(CompletedExerciseType.OVERHEAD_PRESS, 37.5),
        ),
    ),
    CompletedWorkoutRecord(
        id = 3L,
        workout = CompletedWorkoutType.A,
        startedAtEpochMillis = 1_727_251_200_000L,
        completedAtEpochMillis = 1_727_253_840_000L,
        exercises = listOf(
            historyExercise(CompletedExerciseType.SQUAT, 77.5),
            historyExercise(CompletedExerciseType.BARBELL_ROW, 60.0),
            historyExercise(CompletedExerciseType.BENCH_PRESS, 45.0),
        ),
    ),
)

private fun historyExercise(
    exercise: CompletedExerciseType,
    weightKg: Double,
    sets: Int = 5,
) = CompletedWorkoutExercise(
    exercise = exercise,
    sets = sets,
    reps = 5,
    weightKg = weightKg,
    setStates = List(sets) { CompletedWorkoutSet(it, true) },
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
