package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Workout
import kotlin.test.Test
import kotlin.test.assertEquals

internal class WorkoutFormattingTest {
    @Test
    fun durationUsesWholeElapsedSecondsAndSupportsSessionsLongerThanAnHour() {
        assertEquals("00:00", 0L.formatWorkoutDuration())
        assertEquals("00:59", 59_999L.formatWorkoutDuration())
        assertEquals("01:00", 60_000L.formatWorkoutDuration())
        assertEquals("42:18", 2_538_000L.formatWorkoutDuration())
        assertEquals("1:00:00", 3_600_000L.formatWorkoutDuration())
        assertEquals("2:01:09", 7_269_000L.formatWorkoutDuration())
    }

    @Test
    fun backwardClockChangeDoesNotDisplayANegativeDuration() {
        val saved = CompletedWorkout(1L, Workout.A, 2_000L, 1_000L, emptyList())
        assertEquals(0L, saved.durationMillis)
        assertEquals("00:00", saved.durationMillis.formatWorkoutDuration())
    }
}
