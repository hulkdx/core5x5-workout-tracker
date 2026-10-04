package com.hulkdx.core5x5.feature.workout.domain

internal interface WorkoutRepository {
    /** Starts the selected program, or returns the existing session without changing it. */
    suspend fun startWorkout(workout: Workout): UnfinishedWorkout
}
