package com.hulkdx.core5x5.feature.workout.domain

/** The fixed program's prescriptions. Weight defaults are in kilograms. */
internal enum class Exercise(
    val sets: Int,
    val reps: Int,
    val startingWeightKg: Double,
) {
    SQUAT(sets = 5, reps = 5, startingWeightKg = 20.0),
    BENCH_PRESS(sets = 5, reps = 5, startingWeightKg = 20.0),
    BARBELL_ROW(sets = 5, reps = 5, startingWeightKg = 20.0),
    OVERHEAD_PRESS(sets = 5, reps = 5, startingWeightKg = 20.0),
    DEADLIFT(sets = 1, reps = 5, startingWeightKg = 20.0),
}
