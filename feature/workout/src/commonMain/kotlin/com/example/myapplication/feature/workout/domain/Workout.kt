package com.example.myapplication.feature.workout.domain

/** The two fixed workouts, with exercises in training order. */
internal enum class Workout(val exercises: List<Exercise>) {
    A(listOf(Exercise.SQUAT, Exercise.BENCH_PRESS, Exercise.BARBELL_ROW)),
    B(listOf(Exercise.SQUAT, Exercise.OVERHEAD_PRESS, Exercise.DEADLIFT)),
}
