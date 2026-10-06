package com.hulkdx.core5x5.feature.workout.domain

internal interface WorkoutRepository {
    /** Returns the persisted session as stored, or null when no workout is active. */
    suspend fun getUnfinishedWorkout(): UnfinishedWorkout?

    /** Starts the selected program, or returns the existing session without changing it. */
    suspend fun startWorkout(workout: Workout): UnfinishedWorkout

    /**
     * Completes the identified unfinished session, preserving its logged sets.
     * Returns false if it does not exist or was already completed, leaving all sessions unchanged.
     */
    suspend fun finalizeWorkout(workoutId: Long): Boolean

    /**
     * Sets completion for one set in the current session using zero-based exercise/set positions.
     * Returns false if no matching set exists. Repeating the same value is safe.
     */
    suspend fun setSetCompleted(
        exercisePosition: Int,
        setPosition: Int,
        isCompleted: Boolean,
    ): Boolean
}
