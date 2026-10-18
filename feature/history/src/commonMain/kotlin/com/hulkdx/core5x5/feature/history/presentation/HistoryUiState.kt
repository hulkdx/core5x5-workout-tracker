package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord

data class HistoryUiState(
    val isLoading: Boolean = true,
    val completedWorkouts: List<CompletedWorkoutRecord> = emptyList(),
    val error: HistoryError? = null,
    val selectedExercise: CompletedExerciseType = CompletedExerciseType.SQUAT,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val hasPreferencesError: Boolean = false,
)

enum class HistoryError {
    LOAD,
}
