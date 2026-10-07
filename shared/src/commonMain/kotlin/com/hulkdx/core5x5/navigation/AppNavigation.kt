package com.hulkdx.core5x5.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.feature.history.presentation.HistoryRoute
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailRoute
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

    val selectedItem = backStack.last().navigationItem
    val contentInsets = if (selectedItem != null) {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        )
    } else {
        // Workout destinations own their safe insets and have no bottom navigation.
        Modifier
    }

    Column(modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background)) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).then(contentInsets)) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<AppDestination.Today>(clazzContentKey = { "today" }) {
                        ShellScreen(uiState = uiState, onRetryPreferences = viewModel::loadPreferences) {
                            TodayRoute(
                                weightUnit = uiState.weightUnit,
                                onWorkoutRequested = { workoutId ->
                                    backStack.add(AppDestination.ActiveWorkout(workoutId))
                                },
                            )
                        }
                    }
                    entry<AppDestination.History>(clazzContentKey = { "history" }) {
                        ShellScreen(
                            uiState = uiState.copy(title = "History"),
                            showTitle = false,
                            onRetryPreferences = viewModel::loadPreferences,
                        ) {
                            HistoryRoute(
                                weightUnit = uiState.weightUnit,
                                onWorkoutSelected = { workoutId ->
                                    backStack.add(AppDestination.WorkoutDetail(workoutId))
                                },
                            )
                        }
                    }
                    entry<AppDestination.Settings>(clazzContentKey = { "settings" }) {
                        ShellScreen(
                            uiState = uiState,
                            showTitle = false,
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
                                // Replace Active so Back cannot reopen a saved workout.
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
                    entry<AppDestination.WorkoutDetail>(clazzContentKey = { "detail:${it.workoutId}" }) { destination ->
                        WorkoutDetailRoute(
                            workoutId = destination.workoutId,
                            weightUnit = uiState.weightUnit,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
        if (selectedItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Core5x5Colors.Elevated)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ),
            ) {
                Core5x5BottomNavigation(
                    selectedItem = selectedItem,
                    onItemSelected = { item ->
                        if (backStack.lastOrNull()?.navigationItem != null) {
                            backStack.selectNavigationItem(item)
                            if (item == Core5x5NavigationItem.TODAY) viewModel.loadPreferences()
                        }
                    },
                )
            }
        }
    }
}
