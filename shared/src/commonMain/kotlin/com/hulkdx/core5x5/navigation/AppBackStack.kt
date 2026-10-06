package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem

internal sealed interface AppDestination {
    data object Today : AppDestination
    data object History : AppDestination
    data object Settings : AppDestination
    data class ActiveWorkout(val workoutId: Long) : AppDestination
    data class WorkoutComplete(val workoutId: Long) : AppDestination
}

internal val AppDestination.navigationItem: Core5x5NavigationItem?
    get() = when (this) {
        AppDestination.Today -> Core5x5NavigationItem.TODAY
        AppDestination.History -> Core5x5NavigationItem.HISTORY
        AppDestination.Settings -> Core5x5NavigationItem.SETTINGS
        is AppDestination.ActiveWorkout, is AppDestination.WorkoutComplete -> null
    }

internal fun SnapshotStateList<AppDestination>.selectNavigationItem(item: Core5x5NavigationItem) {
    // Ignore a delayed tab callback after entering the focused workout flow.
    if (lastOrNull()?.navigationItem == null) return
    val destination = when (item) {
        Core5x5NavigationItem.TODAY -> AppDestination.Today
        Core5x5NavigationItem.HISTORY -> AppDestination.History
        Core5x5NavigationItem.SETTINGS -> AppDestination.Settings
    }
    if (lastOrNull() == destination) return

    // Keep Today as the root. Tab switching replaces the other tab instead of accumulating visits.
    while (size > 1) removeLastOrNull()
    if (destination != AppDestination.Today) add(destination)
}

internal val AppBackStackSaver = listSaver<SnapshotStateList<AppDestination>, String>(
    save = { destinations ->
        destinations.map { destination ->
            when (destination) {
                AppDestination.Today -> "today"
                AppDestination.History -> "history"
                AppDestination.Settings -> "settings"
                is AppDestination.ActiveWorkout -> "active:${destination.workoutId}"
                is AppDestination.WorkoutComplete -> "complete:${destination.workoutId}"
            }
        }
    },
    restore = { keys ->
        keys.map { key ->
            when {
                key == "today" -> AppDestination.Today
                key == "history" -> AppDestination.History
                key == "settings" -> AppDestination.Settings
                key.startsWith("active:") -> AppDestination.ActiveWorkout(key.substringAfter(':').toLong())
                key.startsWith("complete:") -> AppDestination.WorkoutComplete(key.substringAfter(':').toLong())
                else -> error("Unknown saved destination: $key")
            }
        }.toMutableStateList()
    },
)
