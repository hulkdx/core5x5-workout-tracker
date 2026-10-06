package com.hulkdx.core5x5.feature.workout.domain

/** A completed session reloaded from storage, including its original prescriptions and logged sets. */
internal data class CompletedWorkout(
    val id: Long,
    val workout: Workout,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val exercises: List<UnfinishedWorkoutExercise>,
) {
    val durationMillis: Long
        get() = (completedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)

    val prescribedSets: Int
        get() = exercises.sumOf { it.sets }

    val completedSets: Int
        get() = exercises.sumOf { exercise -> exercise.setStates.count { it.isCompleted } }
}
