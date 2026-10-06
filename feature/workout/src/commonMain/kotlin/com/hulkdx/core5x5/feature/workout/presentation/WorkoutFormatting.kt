package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.Exercise

internal fun Exercise.displayName(): String = when (this) {
    Exercise.SQUAT -> "Squat"
    Exercise.BENCH_PRESS -> "Bench Press"
    Exercise.BARBELL_ROW -> "Barbell Row"
    Exercise.OVERHEAD_PRESS -> "Overhead Press"
    Exercise.DEADLIFT -> "Deadlift"
}

/** Elapsed duration from saved timestamps, never a clock that continues running on the summary. */
internal fun Long.formatWorkoutDuration(): String {
    val seconds = coerceAtLeast(0L) / 1_000
    val minutes = seconds / 60
    val remainingSeconds = (seconds % 60).toString().padStart(2, '0')
    return if (minutes < 60) {
        "${minutes.toString().padStart(2, '0')}:$remainingSeconds"
    } else {
        "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$remainingSeconds"
    }
}
