package com.hulkdx.core5x5.core.training.domain

/** Mutation used by History; the workout storage owner performs the durable write. */
interface CompletedWorkoutDeletion {
    /**
     * Deletes this completed session and its snapshots atomically.
     * Returns false for missing or unfinished IDs. Repeating a committed delete is safe.
     * Storage failures propagate; the caller must refresh stale IDs rather than delete another session.
     */
    suspend fun deleteCompletedWorkout(workoutId: Long): Boolean
}
