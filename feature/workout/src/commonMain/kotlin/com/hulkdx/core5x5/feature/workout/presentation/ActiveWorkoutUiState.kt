package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout

internal data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val hasLoadError: Boolean = false,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val isCompletingSet: Boolean = false,
    val hasSetSaveError: Boolean = false,
    val selectedExercisePosition: Int = 0,
    val restTimer: RestTimerUiState = RestTimerUiState(),
    /** Navigation is requested only after the identified session is read back as completed. */
    val requestedCompletedWorkoutId: Long? = null,
) {
    val canFinish: Boolean
        get() = canCompleteSet

    val canCompleteSet: Boolean
        get() = !isLoading && !hasLoadError && !isSaving && !isCompletingSet &&
            unfinishedWorkout != null && requestedCompletedWorkoutId == null
}
