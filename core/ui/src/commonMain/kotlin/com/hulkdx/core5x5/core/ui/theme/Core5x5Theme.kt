package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// The shell retains minimal Material styling; implemented components use explicit Core5x5 tokens.
@Composable
fun Core5x5Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
