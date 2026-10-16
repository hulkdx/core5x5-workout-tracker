package com.hulkdx.core5x5.feature.workout.domain

internal data class UnfinishedWorkout(
    val workout: Workout,
    val startedAtEpochMillis: Long,
    val exercises: List<UnfinishedWorkoutExercise>,
    val id: Long,
    val restTimer: RestTimer? = null,
)

/** Prescriptions copied when the session starts, in training order. */
internal data class UnfinishedWorkoutExercise(
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val setStates: List<UnfinishedWorkoutSet> = List(sets) { UnfinishedWorkoutSet(it) },
    val customName: String? = null,
    /** Null follows Settings, zero disables rest, positive values override it for this exercise. */
    val restDurationMillis: Long? = null,
)

/** Null reps means unrecorded; a positive count remains completed even below the target. */
internal data class UnfinishedWorkoutSet(
    val position: Int,
    val isCompleted: Boolean = false,
    val completedReps: Int? = null,
) {
    fun nextCompletedReps(targetReps: Int): Int? {
        require(targetReps > 0)
        if (!isCompleted) return targetReps
        return ((completedReps ?: targetReps) - 1).takeIf { it > 0 }
    }
}
