package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.feature.workout.domain.RestTimerState

internal data class RestTimerUiState(
    val isVisible: Boolean = false,
    val isExpired: Boolean = false,
    val countdown: String = "00:00",
)

internal fun RestTimerState.toUiState(): RestTimerUiState = when (this) {
    RestTimerState.Idle -> RestTimerUiState()
    RestTimerState.Expired -> RestTimerUiState(isVisible = true, isExpired = true)
    is RestTimerState.Running -> {
        // Round up so a running timer never displays 00:00. Avoid overflow at Long.MAX_VALUE.
        val seconds = remainingMillis / 1_000 + if (remainingMillis % 1_000 > 0) 1 else 0
        RestTimerUiState(
            isVisible = true,
            countdown = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}",
        )
    }
}
