package com.hulkdx.core5x5.feature.workout.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UnfinishedWorkoutSetTest {
    @Test
    fun singleTapCycleUsesTheConfiguredTargetAndResetsAfterOne() {
        for (target in listOf(1, 5, 8, 100)) {
            var set = UnfinishedWorkoutSet(0)
            for (expected in target downTo 1) {
                val actual = set.nextCompletedReps(target)
                assertEquals(expected, actual)
                set = set.copy(isCompleted = true, completedReps = actual)
            }
            assertNull(set.nextCompletedReps(target))
            val reset = set.copy(isCompleted = false, completedReps = null)
            assertEquals(target, reset.nextCompletedReps(target))
        }
    }

    @Test
    fun changingTheTargetDoesNotRewriteAlreadyRecordedReps() {
        assertEquals(3, UnfinishedWorkoutSet(0, true, 4).nextCompletedReps(2))
        assertEquals(1, UnfinishedWorkoutSet(0, true, 2).nextCompletedReps(8))
        assertNull(UnfinishedWorkoutSet(0, true, 1).nextCompletedReps(8))
        assertFailsWith<IllegalArgumentException> { UnfinishedWorkoutSet(0).nextCompletedReps(0) }
    }
}
