package com.hulkdx.core5x5.feature.workout.domain

/** Session-local values; canonical exercise identity is never replaced. */
internal data class ExerciseEdit(
    val name: String,
    val weightKg: Double,
    val sets: Int,
    val reps: Int,
    val restDurationMillis: Long?,
) {
    init {
        require(name.isNotBlank() && name.length <= 80)
        require(weightKg.isFinite() && weightKg in 0.0..1_000_000.0)
        require(sets in 1..100 && reps in 1..100)
        require(restDurationMillis == null || restDurationMillis >= 0)
    }
}
