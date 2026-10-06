package com.hulkdx.core5x5.feature.workout.domain

internal data class UnfinishedWorkout(
    val workout: Workout,
    val startedAtEpochMillis: Long,
    val exercises: List<UnfinishedWorkoutExercise>,
)

/** Prescriptions copied when the session starts, in training order. */
internal data class UnfinishedWorkoutExercise(
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val setStates: List<UnfinishedWorkoutSet> = List(sets) { UnfinishedWorkoutSet(it) },
)

/** An individual set's zero-based position and saved completion state. */
internal data class UnfinishedWorkoutSet(
    val position: Int,
    val isCompleted: Boolean = false,
)
