package com.hulkdx.core5x5.feature.workout.domain

internal interface WorkoutRepository {
    /** Returns the persisted session as stored, or null when no workout is active. */
    suspend fun getUnfinishedWorkout(): UnfinishedWorkout?

    /** Returns only a durably completed session with this ID; active or missing IDs return null. */
    suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout?

    /**
     * Returns the program after the latest completed session, or A before any completion.
     * Unfinished sessions and set completion do not advance this selection.
     */
    suspend fun getNextWorkout(): Workout

    /** The next canonical program, repeating each lift's latest saved weight without progression. */
    suspend fun getNextWorkoutPrescription(): WorkoutPrescription

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
