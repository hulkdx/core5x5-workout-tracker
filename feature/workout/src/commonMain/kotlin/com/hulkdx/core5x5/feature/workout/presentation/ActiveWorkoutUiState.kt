package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout

internal data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val hasLoadError: Boolean = false,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    /** Navigation is requested only after the identified session is read back as completed. */
    val requestedCompletedWorkoutId: Long? = null,
) {
    val canFinish: Boolean
        get() = !isLoading && !isSaving && unfinishedWorkout != null && requestedCompletedWorkoutId == null
}
