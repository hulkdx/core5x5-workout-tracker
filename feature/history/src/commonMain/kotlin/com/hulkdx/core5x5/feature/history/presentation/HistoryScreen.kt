package com.hulkdx.core5x5.feature.history.presentation

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
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onWorkoutSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // TODO: Task 5.4 — chronological completed-session list and detail selection.
    }
}

// Preview viewport: design/tokens.json reference.width and reference.height.
@Preview(name = "History scaffold", widthDp = 390, heightDp = 844)
@Composable
private fun HistoryScreenPreview() {
    Core5x5Theme {
        HistoryScreen(
            uiState = HistoryUiState,
            onWorkoutSelected = {},
            modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background),
        )
    }
}
