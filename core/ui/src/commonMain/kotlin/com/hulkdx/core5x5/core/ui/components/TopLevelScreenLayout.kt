package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppSettingsTokens

/** Shared top-level chrome. Each route keeps its own layout intact during entry transitions. */
@Composable
fun TopLevelScreenLayout(
    selectedTab: TopLevelTab,
    onTabSelected: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable () -> Unit,
) {
    val background = when (selectedTab) {
        TopLevelTab.TODAY -> AppHomeTokens.Background
        TopLevelTab.HISTORY -> AppHistoryTokens.Background
        TopLevelTab.SETTINGS -> AppSettingsTokens.Background
    }
    Column(modifier = modifier.fillMaxSize().background(background)) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
        ) { content() }
        Box(
            modifier = Modifier.fillMaxWidth().background(AppHomeTokens.Card).windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
        ) {
            AppBottomNavigation(selectedItem = selectedTab, onItemSelected = onTabSelected, useHomeDesign = true)
        }
    }
}
