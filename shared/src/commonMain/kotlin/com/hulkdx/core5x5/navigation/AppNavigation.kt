package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hulkdx.core5x5.core.ui.components.Core5x5Tab
import com.hulkdx.core5x5.feature.settings.presentation.SettingsRoute
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutRoute
import com.hulkdx.core5x5.feature.workout.presentation.TodayRoute
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteRoute
import com.hulkdx.core5x5.shell.ShellScreen
import com.hulkdx.core5x5.shell.ShellViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AppNavigation(viewModel: ShellViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberSaveable(saver = AppBackStackSaver) {
        mutableStateListOf<AppDestination>(AppDestination.Today)
    }
    val onSelectTab: (Core5x5Tab) -> Unit = { tab ->
        when (tab) {
            Core5x5Tab.TODAY -> {
                while (backStack.size > 1) backStack.removeLastOrNull()
                viewModel.loadPreferences()
            }
            Core5x5Tab.SETTINGS -> {
                if (backStack.lastOrNull() != AppDestination.Settings) backStack.add(AppDestination.Settings)
            }
            Core5x5Tab.HISTORY -> Unit // Task 5.4 connects History after its data/state/UI are implemented.
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.Today>(clazzContentKey = { "today" }) {
                ShellScreen(uiState = uiState, onSelectTab = onSelectTab, onRetryPreferences = viewModel::loadPreferences) {
                    TodayRoute(
                        weightUnit = uiState.weightUnit,
                        onWorkoutRequested = { workoutId -> backStack.add(AppDestination.ActiveWorkout(workoutId)) },
                    )
                }
            }
            entry<AppDestination.Settings>(clazzContentKey = { "settings" }) {
                ShellScreen(
                    uiState = uiState,
                    selectedTab = Core5x5Tab.SETTINGS,
                    onSelectTab = onSelectTab,
                    onRetryPreferences = viewModel::loadPreferences,
                ) {
                    SettingsRoute()
                }
            }
            entry<AppDestination.ActiveWorkout>(clazzContentKey = { "active:${it.workoutId}" }) { destination ->
                ActiveWorkoutRoute(
                    workoutId = destination.workoutId,
                    weightUnit = uiState.weightUnit,
                    onBack = { backStack.removeLastOrNull() },
                    onWorkoutCompleted = { workoutId ->
                        // Replace Active so neither system Back nor the return action reopens a saved workout.
                        if (backStack.lastOrNull() == destination) {
                            backStack[backStack.lastIndex] = AppDestination.WorkoutComplete(workoutId)
                        }
                    },
                )
            }
            entry<AppDestination.WorkoutComplete>(clazzContentKey = { "complete:${it.workoutId}" }) { destination ->
                WorkoutCompleteRoute(
                    workoutId = destination.workoutId,
                    weightUnit = uiState.weightUnit,
                    onBackToToday = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

internal sealed interface AppDestination {
    data object Today : AppDestination
    data object Settings : AppDestination
    data class ActiveWorkout(val workoutId: Long) : AppDestination
    data class WorkoutComplete(val workoutId: Long) : AppDestination
}

internal val AppBackStackSaver = listSaver<SnapshotStateList<AppDestination>, String>(
    save = { destinations ->
        destinations.map { destination ->
            when (destination) {
                AppDestination.Today -> "today"
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
                key == "settings" -> AppDestination.Settings
                key.startsWith("active:") -> AppDestination.ActiveWorkout(key.substringAfter(':').toLong())
                key.startsWith("complete:") -> AppDestination.WorkoutComplete(key.substringAfter(':').toLong())
                else -> error("Unknown saved destination: $key")
            }
        }.toMutableStateList()
    },
)
