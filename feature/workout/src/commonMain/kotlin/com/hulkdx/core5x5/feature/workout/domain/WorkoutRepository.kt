package com.hulkdx.core5x5.feature.workout.domain

internal interface WorkoutRepository {
    /** Returns the persisted session as stored, or null when no workout is active. */
    suspend fun getUnfinishedWorkout(): UnfinishedWorkout?

    /** Starts the selected program, or returns the existing session without changing it. */
    suspend fun startWorkout(workout: Workout): UnfinishedWorkout
}
