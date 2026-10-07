package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HistoryFormattingTest {
    @Test
    fun datesUseTheSavedUtcCalendarDate() {
        val newYear = record(completedAt = 0L)
        val leapDay = record(completedAt = 1_582_934_400_000L)

        assertEquals("Jan 1", newYear.dateLabel())
        assertEquals("January 1, 1970", newYear.detailDateLabel())
        assertEquals("Feb 29", leapDay.dateLabel())
        assertEquals("February 29, 2020", leapDay.detailDateLabel())
    }

    @Test
    fun durationsUseCompactListAndClockLabels() {
        val workout = record(
            startedAt = 0L,
            completedAt = 3_661_000L,
        )

        assertEquals("1 h 1 min", workout.durationLabel())
        assertEquals("01:01:01", workout.durationClockLabel())
    }

    private fun record(
        startedAt: Long = 0L,
        completedAt: Long,
    ) = CompletedWorkoutRecord(
        id = 1L,
        workout = CompletedWorkoutType.A,
        startedAtEpochMillis = startedAt,
        completedAtEpochMillis = completedAt,
        exercises = emptyList(),
    )
}
