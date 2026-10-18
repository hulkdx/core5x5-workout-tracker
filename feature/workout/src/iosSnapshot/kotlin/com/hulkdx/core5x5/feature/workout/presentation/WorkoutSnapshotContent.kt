package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

/** Compiled only with -PiosSnapshots=true; feature internals stay inside their module. */
@Composable
fun WorkoutSnapshotContent(scene: String) {
    val session = snapshotSession()
    when (scene) {
        "todayA", "todayB" -> TodayScreen(
            uiState = TodayUiState(isLoading = false, nextWorkout = if (scene == "todayA") Workout.A else Workout.B),
            onStartWorkout = {}, onResumeWorkout = {}, onRetryLoad = {},
        )
        "active", "rest" -> ActiveWorkoutScreen(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                unfinishedWorkout = session,
                restTimer = RestTimerUiState(isVisible = scene == "rest", countdown = "02:30"),
            ),
            onBack = {}, onFinishWorkout = {}, onRetryLoad = {}, onToggleSet = { _, _ -> }, onEditExercise = {},
        )
        "editor" -> ExerciseEditScreen(
            state = ExerciseEditUiState(0, "Squat", 80.0, WeightUnit.KG, 5, 5, null, 180_000),
            onDismiss = {}, onSave = {}, onWeightChanged = {}, onSetsChanged = {},
            onRepsChanged = {}, onRestSelected = {}, onOpenCustomRest = {}, onCustomRestChanged = {}, onApplyCustomRest = {},
        )
        "complete" -> WorkoutCompleteScreen(
            uiState = WorkoutCompleteUiState(
                isLoading = false,
                completedWorkout = CompletedWorkout(1L, Workout.A, 0L, 2_538_000L, session.exercises),
                nextWorkout = WorkoutPrescription(Workout.B),
            ),
            onRetryLoad = {}, onBackToToday = {},
        )
        else -> error("Unknown workout snapshot: $scene")
    }
}

private fun snapshotSession() = UnfinishedWorkout(
    workout = Workout.A,
    startedAtEpochMillis = 0L,
    id = 1L,
    exercises = Workout.A.exercises.mapIndexed { index, exercise ->
        UnfinishedWorkoutExercise(
            exercise = exercise,
            sets = exercise.sets,
            reps = exercise.reps,
            weightKg = listOf(80.0, 37.5, 60.0)[index],
            setStates = List(exercise.sets) { position ->
                val reps = if (index == 0) listOf(5, 4, 1, null, null)[position] else null
                UnfinishedWorkoutSet(position, isCompleted = reps != null, completedReps = reps)
            },
        )
    },
)
