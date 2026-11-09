package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import kotlin.math.abs

data class HistoryWeightPoint(
    val workoutId: Long,
    val completedAtEpochMillis: Long,
    val weightKg: Double,
)

/** Saved weight used in sessions with logged sets, including partial-rep sets. */
fun List<CompletedWorkoutRecord>.weightHistory(
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
fun List<HistoryWeightPoint>.timeFraction(index: Int): Float {
    val start = first().completedAtEpochMillis.toDouble()
    val span = last().completedAtEpochMillis.toDouble() - start
    return if (span == 0.0) 0.5f else ((this[index].completedAtEpochMillis.toDouble() - start) / span).toFloat()
}

/** Find the nearest point; repeated taps cycle through overlapping records. */
fun List<HistoryWeightPoint>.closestWeightPointIndex(
    tapX: Float,
    tapY: Float,
    plotWidth: Float,
    plotHeight: Float,
    pointInset: Float,
    touchRadius: Float,
    selectedWorkoutId: Long? = null,
): Int? {
    if (isEmpty() || plotWidth <= 0f || plotHeight <= 0f) return null
    val maximum = maxOf { it.weightKg }.takeIf { it > 0.0 } ?: 1.0
    val width = (plotWidth - pointInset * 2).coerceAtLeast(0f)
    val height = (plotHeight - pointInset * 2).coerceAtLeast(0f)
    val distances = indices.map { index ->
        val dx = tapX - (pointInset + timeFraction(index) * width)
        val dy = tapY - (pointInset + (1.0 - this[index].weightKg / maximum).toFloat() * height)
        dx * dx + dy * dy
    }
    val nearestDistance = distances.minOrNull() ?: return null
    if (nearestDistance > touchRadius * touchRadius) return null
    val tied = indices.filter { abs(distances[it] - nearestDistance) < 0.01f }
    val selectedIndex = tied.indexOfFirst { this[it].workoutId == selectedWorkoutId }
    return tied[if (selectedIndex >= 0) (selectedIndex + 1) % tied.size else 0]
}
