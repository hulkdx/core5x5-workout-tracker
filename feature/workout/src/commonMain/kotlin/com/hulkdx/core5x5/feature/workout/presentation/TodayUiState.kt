package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

internal data class TodayUiState(
    val isLoading: Boolean = true,
    val nextWorkout: Workout? = null,
    val nextWorkoutPrescription: WorkoutPrescription? = null,
    val unfinishedWorkout: UnfinishedWorkout? = null,
    val isWorking: Boolean = false,
    val error: TodayError? = null,
    /** Set only by an explicit start/resume request; loading does not request navigation. */
    val requestedWorkout: UnfinishedWorkout? = null,
) {
    val canStart: Boolean
        get() = !isLoading && !isWorking && nextWorkout != null && requestedWorkout == null

    val canResume: Boolean
        get() = !isLoading && !isWorking && unfinishedWorkout != null && requestedWorkout == null
}

internal enum class TodayError { LOAD, START }
