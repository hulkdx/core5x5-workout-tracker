package com.hulkdx.core5x5.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme

@Composable
internal fun ShellScreen(uiState: ShellUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize().safeContentPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = uiState.title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Preview
@Composable
private fun ShellScreenPreview() {
    Core5x5Theme {
        ShellScreen(uiState = ShellUiState())
    }
}
