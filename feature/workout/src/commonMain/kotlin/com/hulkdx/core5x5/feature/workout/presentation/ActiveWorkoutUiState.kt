package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout

internal data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val hasLoadError: Boolean = false,
)
