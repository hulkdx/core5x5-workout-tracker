package com.hulkdx.core5x5.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme

// Navigation-only previews exclude system insets. Reference dimensions come from design/tokens.json.
@Preview(name = "Today navigation", widthDp = 390, heightDp = 74)
@Composable
private fun TodayNavigationPreview() {
    NavigationPreview(Core5x5NavigationItem.TODAY)
}

@Preview(name = "History navigation", widthDp = 390, heightDp = 74)
@Composable
private fun HistoryNavigationPreview() {
    NavigationPreview(Core5x5NavigationItem.HISTORY)
}

@Preview(name = "Settings navigation", widthDp = 390, heightDp = 74)
@Composable
private fun SettingsNavigationPreview() {
    NavigationPreview(Core5x5NavigationItem.SETTINGS)
}

@Preview(name = "Narrow navigation, larger text", widthDp = 320, heightDp = 74)
@Composable
private fun NarrowNavigationPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
        NavigationPreview(Core5x5NavigationItem.SETTINGS)
    }
}

@Composable
private fun NavigationPreview(selectedItem: Core5x5NavigationItem) {
    Core5x5Theme {
        Core5x5BottomNavigation(selectedItem = selectedItem, onItemSelected = {})
    }
}
