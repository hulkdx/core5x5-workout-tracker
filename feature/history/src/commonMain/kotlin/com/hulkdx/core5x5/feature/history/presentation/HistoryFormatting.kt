package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import kotlin.math.max

internal fun CompletedExerciseType.displayName(): String = when (this) {
    CompletedExerciseType.SQUAT -> "Squat"
    CompletedExerciseType.BENCH_PRESS -> "Bench Press"
    CompletedExerciseType.BARBELL_ROW -> "Barbell Row"
    CompletedExerciseType.OVERHEAD_PRESS -> "Overhead Press"
    CompletedExerciseType.DEADLIFT -> "Deadlift"
}

internal fun CompletedWorkoutRecord.displayName(): String = "Workout ${workout.name}"

internal fun CompletedWorkoutRecord.dateLabel(): String = completedAtEpochMillis.toHistoryDate()

internal fun CompletedWorkoutRecord.detailDateLabel(): String = completedAtEpochMillis.toHistoryDetailDate()

internal fun CompletedWorkoutRecord.durationLabel(): String {
    val totalMinutes = durationMillis / 60_000L
    return if (totalMinutes < 60L) {
        "$totalMinutes min"
    } else {
        "${totalMinutes / 60L} h ${totalMinutes % 60L} min"
    }
}

internal fun CompletedWorkoutRecord.exerciseSummary(unit: WeightUnit): String =
    exercises.joinToString(separator = " · ") { exercise ->
        "${(exercise.customName ?: exercise.exercise.displayName())} ${formatWeight(exercise.weightKg, unit)}"
    }

internal fun CompletedWorkoutRecord.durationClockLabel(): String {
    val totalSeconds = max(0L, durationMillis / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return if (hours == 0L) {
        "${minutes.toString().padStart(2, '0')}:$seconds"
    } else {
        "$hours:${minutes.toString().padStart(2, '0')}:$seconds"
    }
}

private data class CivilDate(val year: Long, val month: Int, val day: Int)

private fun Long.toHistoryDate(): String {
    val date = toCivilDate()
    return "${MONTHS[date.month - 1]} ${date.day}"
}

private fun Long.toHistoryDetailDate(): String {
    val date = toCivilDate()
    return "${MONTH_NAMES[date.month - 1]} ${date.day}, ${date.year}"
}

/** Converts the saved UTC epoch timestamp without introducing a platform date dependency. */
private fun Long.toCivilDate(): CivilDate {
    val days = floorDiv(MILLIS_PER_DAY)
    val z = days + 719_468L
    val era = if (z >= 0L) z / 146_097L else (z - 146_096L) / 146_097L
    val dayOfEra = z - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    val year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPart = (5L * dayOfYear + 2L) / 153L
    val day = (dayOfYear - (153L * monthPart + 2L) / 5L + 1L).toInt()
    val month = (monthPart + if (monthPart < 10L) 3L else -9L).toInt()
    return CivilDate(year + if (month <= 2) 1L else 0L, month, day)
}

private fun Long.floorDiv(divisor: Long): Long {
    val quotient = this / divisor
    return if (this % divisor < 0L) quotient - 1L else quotient
}

private const val MILLIS_PER_DAY = 86_400_000L
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
