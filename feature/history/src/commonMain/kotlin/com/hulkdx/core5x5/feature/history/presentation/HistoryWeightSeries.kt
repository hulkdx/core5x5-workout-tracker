package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord

internal data class HistoryWeightPoint(
    val workoutId: Long,
    val completedAtEpochMillis: Long,
    val weightKg: Double,
)

/** Saved weight used in sessions with logged sets, including partial-rep sets. */
internal fun List<CompletedWorkoutRecord>.weightHistory(
    exercise: CompletedExerciseType,
): List<HistoryWeightPoint> = sortedWith(
    compareBy<CompletedWorkoutRecord> { it.completedAtEpochMillis }.thenBy { it.id },
).mapNotNull { workout ->
    workout.exercises.firstOrNull {
        it.exercise == exercise && it.completedSets > 0 && it.weightKg.isFinite() && it.weightKg >= 0.0
    }?.let { saved ->
        HistoryWeightPoint(workout.id, workout.completedAtEpochMillis, saved.weightKg)
    }
}

/** Proportional elapsed time; a single instant is centered rather than divided by zero. */
internal fun List<HistoryWeightPoint>.timeFraction(index: Int): Float {
    val start = first().completedAtEpochMillis.toDouble()
    val span = last().completedAtEpochMillis.toDouble() - start
    return if (span == 0.0) 0.5f else ((this[index].completedAtEpochMillis.toDouble() - start) / span).toFloat()
}
