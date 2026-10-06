package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

internal data class WorkoutCompleteUiState(
    val isLoading: Boolean = true,
    val completedWorkout: CompletedWorkout? = null,
    val nextWorkout: WorkoutPrescription? = null,
    val error: WorkoutCompleteError? = null,
)

internal enum class WorkoutCompleteError { LOAD, NOT_FOUND }
