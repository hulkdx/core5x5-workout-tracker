package com.hulkdx.core5x5.uitest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.navigation.rememberNavigationState
import com.hulkdx.core5x5.feature.history.navigation.HistoryKey
import com.hulkdx.core5x5.feature.history.navigation.WorkoutDetailKey
import com.hulkdx.core5x5.feature.settings.navigation.SettingsKey
import com.hulkdx.core5x5.feature.workout.navigation.TodayKey
import com.hulkdx.core5x5.navigation.AppNavHost
import com.hulkdx.core5x5.navigation.navigationSavedStateConfiguration

/** A retained destination has no History click entry; seed its stack, then use the real host. */
@Composable
fun RestoredApp(destination: NavKey) {
    val state = rememberNavigationState(
        startKey = TodayKey,
        tabDestinations = mapOf(
            TopLevelTab.TODAY to TodayKey,
            TopLevelTab.HISTORY to HistoryKey,
            TopLevelTab.SETTINGS to SettingsKey,
        ),
        configuration = navigationSavedStateConfiguration,
    )
    val navigator = remember(state) {
        CoreNavigator(state).apply {
            if (destination is WorkoutDetailKey) navigate(HistoryKey)
            navigate(destination)
        }
    }
    AppNavHost(navigator)
}
