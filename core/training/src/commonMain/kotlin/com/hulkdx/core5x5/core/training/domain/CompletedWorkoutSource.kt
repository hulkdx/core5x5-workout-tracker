package com.hulkdx.core5x5.core.training.domain

/**
 * Read-only access to completed workout snapshots shared by presentation features.
 *
 * The owning data feature supplies this source. Consumers must not recreate the
 * Room queries or infer historical values from the current program.
 */
interface CompletedWorkoutSource {
    /** Returns completed sessions in newest saved-date order. */
    suspend fun getCompletedWorkouts(): List<CompletedWorkoutRecord>

    /** Returns a completed session by stable ID, or null for missing/unfinished IDs. */
    suspend fun getCompletedWorkoutById(workoutId: Long): CompletedWorkoutRecord?
}

enum class CompletedWorkoutType {
    A,
    B,
}

enum class CompletedExerciseType {
    SQUAT,
    BENCH_PRESS,
    BARBELL_ROW,
    OVERHEAD_PRESS,
    DEADLIFT,
}

data class CompletedWorkoutSet(
    val position: Int,
    val isCompleted: Boolean,
)

data class CompletedWorkoutExercise(
    val exercise: CompletedExerciseType,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val setStates: List<CompletedWorkoutSet>,
) {
    val completedSets: Int
        get() = setStates.count { it.isCompleted }
}

/** Immutable saved-session data; actual reps are intentionally not represented. */
data class CompletedWorkoutRecord(
    val id: Long,
    val workout: CompletedWorkoutType,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val exercises: List<CompletedWorkoutExercise>,
) {
    val durationMillis: Long
        get() = (completedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)

    val prescribedSets: Int
        get() = exercises.sumOf { it.sets }

    val completedSets: Int
        get() = exercises.sumOf { it.completedSets }
}
