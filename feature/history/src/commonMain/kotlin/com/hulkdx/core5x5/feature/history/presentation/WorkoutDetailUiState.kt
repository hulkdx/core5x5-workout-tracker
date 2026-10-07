package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord

internal data class WorkoutDetailUiState(
    val workoutId: Long,
    val isLoading: Boolean = true,
    val completedWorkout: CompletedWorkoutRecord? = null,
    val error: WorkoutDetailError? = null,
)

internal enum class WorkoutDetailError {
    LOAD,
    NOT_FOUND,
}
