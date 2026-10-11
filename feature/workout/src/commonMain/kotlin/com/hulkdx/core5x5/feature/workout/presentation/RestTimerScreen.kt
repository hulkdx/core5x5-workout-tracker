package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.components.AppRestTimer
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTheme

@Composable
internal fun RestTimerScreen(
    uiState: RestTimerUiState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (uiState.isVisible) {
        AppRestTimer(
            countdown = uiState.countdown,
            isExpired = uiState.isExpired,
            modifier = modifier,
            compact = compact,
        )
    }
}

@Preview(name = "Running rest", widthDp = 390, heightDp = 844)
@Composable
private fun RestTimerScreenPreview() {
    AppTheme {
        RestTimerScreen(
            uiState = RestTimerUiState(isVisible = true, countdown = "02:30"),
            modifier = Modifier.padding(AppDimensions.ScreenInset),
        )
    }
}

@Preview(name = "Expired rest, larger text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun ExpiredRestTimerPreview() {
    AppTheme {
        RestTimerScreen(
            uiState = RestTimerUiState(isVisible = true, isExpired = true),
            modifier = Modifier.padding(AppDimensions.ScreenInset),
            compact = true,
        )
    }
}
