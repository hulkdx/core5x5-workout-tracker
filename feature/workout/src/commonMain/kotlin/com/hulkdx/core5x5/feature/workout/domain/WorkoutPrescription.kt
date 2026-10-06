package com.hulkdx.core5x5.feature.workout.domain

/** An upcoming program has prescriptions but no session identity, timestamps, or logged sets. */
internal data class WorkoutPrescription(
    val workout: Workout,
    val exercises: List<ExercisePrescription> = workout.exercises.map {
        ExercisePrescription(it, it.sets, it.reps, it.startingWeightKg)
    },
)

internal data class ExercisePrescription(
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
)
