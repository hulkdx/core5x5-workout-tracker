package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord

data class WorkoutDetailUiState(
    val workoutId: Long,
    val isLoading: Boolean = true,
    val completedWorkout: CompletedWorkoutRecord? = null,
    val error: WorkoutDetailError? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val hasPreferencesError: Boolean = false,
)

enum class WorkoutDetailError {
    LOAD,
    NOT_FOUND,
}
