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
internal fun WorkoutDetailScreen(
    uiState: WorkoutDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // TODO: Task 5.7 — read-only saved-session details after the task 5.5 specification.
    }
}

// Preview viewport: design/tokens.json reference.width and reference.height.
@Preview(name = "Workout detail scaffold", widthDp = 390, heightDp = 844)
@Composable
private fun WorkoutDetailScreenPreview() {
    Core5x5Theme {
        WorkoutDetailScreen(
            uiState = WorkoutDetailUiState(workoutId = 1L),
            onBack = {},
            modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background),
        )
    }
}
