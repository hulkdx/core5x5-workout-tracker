package com.hulkdx.core5x5.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.AppBottomNavigation
import com.hulkdx.core5x5.core.ui.theme.AppTheme

// Navigation-only previews exclude system insets.
@Preview(name = "Today navigation", widthDp = 390, heightDp = 74)
@Composable
private fun TodayNavigationPreview() {
    NavigationPreview(TopLevelTab.TODAY)
}

@Preview(name = "History navigation", widthDp = 390, heightDp = 74)
@Composable
private fun HistoryNavigationPreview() {
    NavigationPreview(TopLevelTab.HISTORY)
}

@Preview(name = "Settings navigation", widthDp = 390, heightDp = 74)
@Composable
private fun SettingsNavigationPreview() {
    NavigationPreview(TopLevelTab.SETTINGS)
}

@Preview(name = "Narrow navigation, larger text", widthDp = 320, heightDp = 110)
@Composable
private fun NarrowNavigationPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        NavigationPreview(TopLevelTab.SETTINGS)
    }
}

@Composable
private fun NavigationPreview(selectedItem: TopLevelTab) {
    AppTheme {
        AppBottomNavigation(selectedItem = selectedItem, onItemSelected = {})
    }
}
