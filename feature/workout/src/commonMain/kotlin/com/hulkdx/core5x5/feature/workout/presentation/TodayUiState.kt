package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

data class TodayUiState(
    val isLoading: Boolean = true,
    val nextWorkout: Workout? = null,
    val nextWorkoutPrescription: WorkoutPrescription? = null,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val previousWorkout: CompletedWorkout? = null,
    val isWorking: Boolean = false,
    val isReopening: Boolean = false,
    /** Remains true after the request is consumed, until departure ends or navigation is rejected. */
    val isNavigating: Boolean = false,
    val error: TodayError? = null,
    /** Set only by an explicit start/resume request; loading does not request navigation. */
    val requestedWorkout: UnfinishedWorkout? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val hasPreferencesError: Boolean = false,
) {
    val canStart: Boolean
        get() = !isLoading && !isWorking && !isNavigating && nextWorkout != null && requestedWorkout == null && error != TodayError.REOPEN

    val canResume: Boolean
        get() = !isLoading && !isWorking && !isNavigating && unfinishedWorkout != null && requestedWorkout == null

    val canResumePrevious: Boolean
        get() = !isLoading && !isWorking && !isNavigating && unfinishedWorkout == null && previousWorkout != null && requestedWorkout == null
}

enum class TodayError { LOAD, START, SWITCH, REOPEN }
