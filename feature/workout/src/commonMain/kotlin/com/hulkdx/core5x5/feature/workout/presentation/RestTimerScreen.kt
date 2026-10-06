package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun RestTimerScreen(
    uiState: RestTimerUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // TODO: Task 4.5 — reusable non-blocking timer content and Skip/extend callbacks.
    }
}

// Isolated preview for the future embedded Active Workout timer surface.
// Preview viewport: design/tokens.json reference.width and reference.height.
@Preview(name = "Rest timer scaffold", widthDp = 390, heightDp = 844)
@Composable
private fun RestTimerScreenPreview() {
    Core5x5Theme {
        RestTimerScreen(
            uiState = RestTimerUiState,
            modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background),
        )
    }
}
