package com.hulkdx.core5x5.app

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.navigation.rememberCoreNavigator
import com.hulkdx.core5x5.core.navigation.rememberNavigationState
import com.hulkdx.core5x5.feature.history.navigation.HistoryKey
import com.hulkdx.core5x5.feature.settings.navigation.SettingsKey
import com.hulkdx.core5x5.feature.workout.navigation.TodayKey
import com.hulkdx.core5x5.navigation.AppNavHost
import com.hulkdx.core5x5.navigation.navigationSavedStateConfiguration

@Composable
fun AppRoot() {
    val navigator = rememberCoreNavigator(
        rememberNavigationState(
            startKey = TodayKey,
            tabDestinations = mapOf(
                TopLevelTab.TODAY to TodayKey,
                TopLevelTab.HISTORY to HistoryKey,
                TopLevelTab.SETTINGS to SettingsKey,
            ),
            configuration = navigationSavedStateConfiguration,
        ),
    )
    AppNavHost(navigator)
}
