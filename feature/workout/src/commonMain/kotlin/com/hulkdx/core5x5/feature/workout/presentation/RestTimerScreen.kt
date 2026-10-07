package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.components.Core5x5RestTimer
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme

@Composable
internal fun RestTimerScreen(
    uiState: RestTimerUiState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (uiState.isVisible) {
        Core5x5RestTimer(
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
    Core5x5Theme {
        RestTimerScreen(
            uiState = RestTimerUiState(isVisible = true, countdown = "02:30"),
            modifier = Modifier.padding(Core5x5Dimensions.ScreenInset),
        )
    }
}

@Preview(name = "Expired rest, larger text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun ExpiredRestTimerPreview() {
    Core5x5Theme {
        RestTimerScreen(
            uiState = RestTimerUiState(isVisible = true, isExpired = true),
            modifier = Modifier.padding(Core5x5Dimensions.ScreenInset),
            compact = true,
        )
    }
}
