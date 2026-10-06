package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.RestTimerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RestTimerUiStateTest {
    @Test
    fun runningTimeRoundsUpAndExpiryRemainsVisibleAtZero() {
        assertFalse(RestTimerState.Idle.toUiState().isVisible)
        assertEquals("03:00", RestTimerState.Running(180_000).toUiState().countdown)
        assertEquals("03:00", RestTimerState.Running(179_999).toUiState().countdown)
        assertEquals("02:59", RestTimerState.Running(179_000).toUiState().countdown)
        assertEquals("00:01", RestTimerState.Running(1).toUiState().countdown)
        assertEquals("00:00", RestTimerState.Expired.toUiState().countdown)
        assertTrue(RestTimerState.Expired.toUiState().isVisible)
        assertTrue(RestTimerState.Expired.toUiState().isExpired)
    }

    @Test
    fun largeBackwardClockChangesDoNotOverflowCountdownRounding() {
        assertEquals("153722867280912:56", RestTimerState.Running(Long.MAX_VALUE).toUiState().countdown)
    }
}
