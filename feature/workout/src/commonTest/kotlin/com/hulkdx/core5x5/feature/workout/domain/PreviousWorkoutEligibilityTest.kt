package com.hulkdx.core5x5.feature.workout.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreviousWorkoutEligibilityTest {
    @Test
    fun usesUtcDaysAndRejectsFutureCompletions() {
        assertTrue(isWorkoutFinishedToday(0L, 86_399_999L))
        assertTrue(isWorkoutFinishedToday(86_400_000L, 86_400_000L))
        assertFalse(isWorkoutFinishedToday(86_399_999L, 86_400_000L))
        assertFalse(isWorkoutFinishedToday(86_400_001L, 86_400_000L))
    }

    @Test
    fun handlesNegativeAndExtremeTimestampsWithoutOverflow() {
        assertTrue(isWorkoutFinishedToday(-86_400_000L, -1L))
        assertFalse(isWorkoutFinishedToday(-1L, 0L))
        assertTrue(isWorkoutFinishedToday(Long.MIN_VALUE, Long.MIN_VALUE + 1))
        assertTrue(isWorkoutFinishedToday(Long.MAX_VALUE - 1, Long.MAX_VALUE))
        assertFalse(isWorkoutFinishedToday(Long.MIN_VALUE, Long.MAX_VALUE))
    }
}
