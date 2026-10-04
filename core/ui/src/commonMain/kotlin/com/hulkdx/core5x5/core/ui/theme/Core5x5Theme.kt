package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Neutral shell styling only; the product design tokens will come with real screens.
@Composable
fun Core5x5Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
