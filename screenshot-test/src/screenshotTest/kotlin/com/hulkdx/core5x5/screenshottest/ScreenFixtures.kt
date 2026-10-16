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
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutUiState
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

/** Matches the supplied month/card composition using real A/B prescriptions. */
internal fun historyReferenceRecords() = listOf(
    Triple(1_791_374_400_000L, 42L, CompletedWorkoutType.A),
    Triple(1_791_115_200_000L, 38L, CompletedWorkoutType.B),
    Triple(1_790_942_400_000L, 40L, CompletedWorkoutType.B),
    Triple(1_790_769_600_000L, 45L, CompletedWorkoutType.A),
    Triple(1_790_510_400_000L, 39L, CompletedWorkoutType.B),
    Triple(1_790_337_600_000L, 41L, CompletedWorkoutType.B),
    Triple(1_790_164_800_000L, 44L, CompletedWorkoutType.A),
).mapIndexed { index, (completedAt, minutes, workout) ->
    CompletedWorkoutRecord(
        id = 7L - index,
        workout = workout,
        startedAtEpochMillis = completedAt - minutes * 60_000L,
        completedAtEpochMillis = completedAt,
        exercises = if (workout == CompletedWorkoutType.A) listOf(
            historyExercise(CompletedExerciseType.SQUAT, 77.5),
            historyExercise(CompletedExerciseType.BENCH_PRESS, 45.0),
            historyExercise(CompletedExerciseType.BARBELL_ROW, 60.0),
        ) else listOf(
            historyExercise(CompletedExerciseType.SQUAT, 77.5),
            historyExercise(CompletedExerciseType.OVERHEAD_PRESS, 37.5),
            historyExercise(CompletedExerciseType.DEADLIFT, 107.5, sets = 1),
        ),
    )
}

private fun historyExercise(
    exercise: CompletedExerciseType,
    weightKg: Double,
    sets: Int = 5,
) = CompletedWorkoutExercise(
    exercise = exercise,
    sets = sets,
    reps = 5,
    weightKg = weightKg,
    setStates = List(sets) { CompletedWorkoutSet(it, true, completedReps = 5) },
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
                val completed = remainingCompleted-- > 0
                UnfinishedWorkoutSet(position, isCompleted = completed,
                    completedReps = if (completed) exercise.reps else null)
            },
        )
    }
}

internal fun activeWorkoutReference(workout: Workout): ActiveWorkoutUiState =
    ActiveWorkoutUiState(
        isLoading = false,
        unfinishedWorkout = unfinishedWorkout(workout).let { session ->
            session.copy(exercises = session.exercises.mapIndexed { index, exercise ->
                exercise.copy(
                    weightKg = listOf(80.0, 37.5, 60.0)[index],
                    setStates = exercise.setStates.map { it.copy(isCompleted = index == 0 && it.position == 0,
                        completedReps = if (index == 0 && it.position == 0) exercise.reps else null) },
                )
            })
        },
    )

/** Snapshot coverage for numeric completion, partial reps, custom targets, and reset. */
internal fun activeWorkoutRepReference(
    targetReps: Int = 5,
    recordedReps: List<Int?> = listOf(5, 4, 3, 2, 1),
): ActiveWorkoutUiState = activeWorkoutReference(Workout.A).let { state ->
    state.copy(unfinishedWorkout = requireNotNull(state.unfinishedWorkout).let { session ->
        session.copy(exercises = session.exercises.mapIndexed { index, exercise ->
            if (index != 0) exercise else exercise.copy(reps = targetReps,
                setStates = exercise.setStates.map { set ->
                    val reps = recordedReps[set.position]
                    set.copy(isCompleted = reps != null, completedReps = reps)
                },
            )
        })
    })
}
