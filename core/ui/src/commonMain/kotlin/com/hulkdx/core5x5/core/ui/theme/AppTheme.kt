package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalDarkTheme = staticCompositionLocalOf { true }

/** Screen tokens resolve here so separate app, preview and test compositions stay isolated. */
@Composable
fun themeColor(dark: Color, light: Color): Color = if (LocalDarkTheme.current) dark else light

@Composable
fun AppTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        val colors = (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
            primary = AppColors.Action,
            onPrimary = AppColors.OnAction,
            primaryContainer = AppColors.ActionTint,
            onPrimaryContainer = AppColors.PrimaryText,
            secondary = AppColors.Action,
            onSecondary = AppColors.OnAction,
            secondaryContainer = AppColors.ActionTint,
            onSecondaryContainer = AppColors.PrimaryText,
            background = AppHomeTokens.Background,
            onBackground = AppColors.PrimaryText,
            surface = AppHomeTokens.Card,
            onSurface = AppColors.PrimaryText,
            surfaceVariant = AppColors.Subtle,
            onSurfaceVariant = AppColors.SecondaryText,
            surfaceTint = Color.Transparent,
            outline = AppColors.Border,
            error = AppColors.Destructive,
        )
        MaterialTheme(colorScheme = colors, content = content)
    }
}
