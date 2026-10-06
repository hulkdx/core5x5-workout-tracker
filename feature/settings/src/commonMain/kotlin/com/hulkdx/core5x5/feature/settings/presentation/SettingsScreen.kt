package com.hulkdx.core5x5.feature.settings.presentation

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
internal fun SettingsScreen(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // TODO: Task 6.3 — state-driven preference rows using the agreed task 6.1 behavior.
    }
}

// Preview viewport: design/tokens.json reference.width and reference.height.
@Preview(name = "Settings scaffold", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    Core5x5Theme {
        SettingsScreen(
            uiState = SettingsUiState,
            modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background),
        )
    }
}
