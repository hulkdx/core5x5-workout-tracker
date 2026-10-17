package com.hulkdx.core5x5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeUIViewController
import com.hulkdx.core5x5.core.ui.components.AppNavigationItem
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.feature.history.presentation.HistorySnapshotContent
import com.hulkdx.core5x5.feature.settings.presentation.SettingsSnapshotContent
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutSnapshotContent
import com.hulkdx.core5x5.navigation.TopLevelDestination

/** Snapshot-only bridge: no Koin, database, preferences, clock, or ViewModel is started. */
fun SnapshotViewController(scene: String, onReady: () -> Unit) = ComposeUIViewController {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, if (scene == "settingsLarge") 2f else 1f)) {
        AppTheme {
            when (scene) {
                "todayA", "todayB" -> SnapshotTab(AppNavigationItem.TODAY) { WorkoutSnapshotContent(scene) }
                "history" -> SnapshotTab(AppNavigationItem.HISTORY) { HistorySnapshotContent() }
                "settings", "settingsLarge" -> SnapshotTab(AppNavigationItem.SETTINGS) { SettingsSnapshotContent() }
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
private fun SnapshotTab(item: AppNavigationItem, content: @Composable () -> Unit) =
    TopLevelDestination(selectedItem = item, onItemSelected = {}, content = content)
