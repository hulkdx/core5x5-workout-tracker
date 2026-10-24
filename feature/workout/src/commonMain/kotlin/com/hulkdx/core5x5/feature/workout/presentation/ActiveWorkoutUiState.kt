package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val hasLoadError: Boolean = false,
    val isSaving: Boolean = false,
    val isNavigating: Boolean = false,
    val hasSaveError: Boolean = false,
    val isCompletingSet: Boolean = false,
    val hasSetSaveError: Boolean = false,
    val isSwitchingWorkout: Boolean = false,
    val hasSwitchError: Boolean = false,
    val selectedExercisePosition: Int = 0,
    val restTimer: RestTimerUiState = RestTimerUiState(),
    val exerciseEdit: ExerciseEditUiState? = null,
    val hasEditLoadError: Boolean = false,
    /** Navigation is requested only after the identified session is read back as completed. */
    val requestedCompletedWorkoutId: Long? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val hasPreferencesError: Boolean = false,
) {
    val canSwitchWorkout: Boolean
        get() = canModifyWorkout

    val canFinish: Boolean
        get() = canCompleteSet

    val canCompleteSet: Boolean
        get() = canModifyWorkout && !hasSwitchError

    // A failed switch may already have committed. Confirm its program before logging by position.
    private val canModifyWorkout: Boolean
        get() = !isLoading && !hasLoadError && !isSaving && !isNavigating && !isCompletingSet && !isSwitchingWorkout &&
            unfinishedWorkout != null && requestedCompletedWorkoutId == null && exerciseEdit == null
}
