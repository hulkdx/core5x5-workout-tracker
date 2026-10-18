package com.hulkdx.core5x5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeUIViewController
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.feature.history.presentation.HistorySnapshotContent
import com.hulkdx.core5x5.feature.settings.presentation.SettingsSnapshotContent
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutSnapshotContent

/** Snapshot-only bridge: no Koin, database, preferences, clock, or ViewModel is started. */
fun SnapshotViewController(scene: String, onReady: () -> Unit) = ComposeUIViewController {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, if (scene == "settingsLarge") 2f else 1f)) {
        AppTheme {
            when (scene) {
                "todayA", "todayB" -> SnapshotTab(TopLevelTab.TODAY) { WorkoutSnapshotContent(scene) }
                "history" -> SnapshotTab(TopLevelTab.HISTORY) { HistorySnapshotContent() }
                "settings", "settingsLarge" -> SnapshotTab(TopLevelTab.SETTINGS) { SettingsSnapshotContent() }
                "active", "rest", "editor", "complete" -> WorkoutSnapshotContent(scene)
                else -> error("Unknown iOS snapshot: $scene")
            }
        }
    }
    LaunchedEffect(Unit) {
        // Allow layout and a subsequent presentation frame before XCTest captures the Metal surface.
        withFrameNanos { }
        withFrameNanos { }
        onReady()
    }
}

@Composable
private fun SnapshotTab(item: TopLevelTab, content: @Composable () -> Unit) =
    TopLevelScreenLayout(selectedTab = item, onTabSelected = {}, content = content)
