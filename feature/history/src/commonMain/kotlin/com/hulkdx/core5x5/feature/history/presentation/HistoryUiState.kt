package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord

internal data class HistoryUiState(
    val isLoading: Boolean = true,
    val completedWorkouts: List<CompletedWorkoutRecord> = emptyList(),
    val error: HistoryError? = null,
    val selectedExercise: CompletedExerciseType = CompletedExerciseType.SQUAT,
)

internal enum class HistoryError {
    LOAD,
}
