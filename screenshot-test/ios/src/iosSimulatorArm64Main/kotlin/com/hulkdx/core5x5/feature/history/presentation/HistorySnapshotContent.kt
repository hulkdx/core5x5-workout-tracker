package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType

@Composable
fun HistorySnapshotContent() = HistoryScreen(
    uiState = HistoryUiState(
        isLoading = false,
        completedWorkouts = listOf(
            CompletedWorkoutRecord(
                id = 1L,
                workout = CompletedWorkoutType.A,
                startedAtEpochMillis = 1_791_374_400_000L - 2_520_000L,
                completedAtEpochMillis = 1_791_374_400_000L,
                exercises = listOf(
                    CompletedExerciseType.SQUAT,
                    CompletedExerciseType.BENCH_PRESS,
                    CompletedExerciseType.BARBELL_ROW,
                ).map { exercise ->
                    CompletedWorkoutExercise(
                        exercise = exercise, sets = 5, reps = 5, weightKg = 20.0,
                        setStates = List(5) { CompletedWorkoutSet(it, true, completedReps = 5) },
                    )
                },
            ),
        ),
    ),
    onRetryLoad = {},
)
