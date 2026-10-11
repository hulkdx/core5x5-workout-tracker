package com.hulkdx.core5x5.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.hulkdx.core5x5.core.ui.components.AppBottomNavigation
import com.hulkdx.core5x5.core.ui.components.AppNavigationItem
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppMotion
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppSettingsTokens
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

    val onItemSelected: (AppNavigationItem) -> Unit = { item ->
        if (backStack.lastOrNull()?.navigationItem != null) {
            backStack.selectNavigationItem(item)
            if (item == AppNavigationItem.TODAY) viewModel.loadPreferences()
        }
    }

    NavDisplay(
        modifier = Modifier.fillMaxSize().background(AppColors.Background),
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        sizeTransform = null,
        transitionSpec = { navigationFade() },
        popTransitionSpec = { navigationFade() },
        // Linear alpha follows the gesture progress and reverses smoothly on cancellation.
        predictivePopTransitionSpec = { navigationFade() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.Today>(clazzContentKey = { "today" }) {
                TopLevelDestination(AppNavigationItem.TODAY, onItemSelected) {
                    ShellScreen(
                        uiState = uiState,
                        showTitle = false,
                        backgroundColor = AppHomeTokens.Background,
                        onRetryPreferences = viewModel::loadPreferences,
                    ) {
                        TodayRoute(
                            weightUnit = uiState.weightUnit,
                            onWorkoutRequested = { workoutId ->
                                backStack.add(AppDestination.ActiveWorkout(workoutId))
                            },
                        )
                    }
                }
            }
            entry<AppDestination.History>(clazzContentKey = { "history" }) {
                TopLevelDestination(AppNavigationItem.HISTORY, onItemSelected) {
                    ShellScreen(
                        uiState = uiState.copy(title = "History"),
                        showTitle = false,
                        backgroundColor = AppHistoryTokens.Background,
                        onRetryPreferences = viewModel::loadPreferences,
                    ) {
                        HistoryRoute()
                    }
                }
            }
            entry<AppDestination.Settings>(clazzContentKey = { "settings" }) {
                TopLevelDestination(AppNavigationItem.SETTINGS, onItemSelected) {
                    ShellScreen(
                        uiState = uiState,
                        showTitle = false,
                        backgroundColor = AppSettingsTokens.Background,
                        onRetryPreferences = viewModel::loadPreferences,
                    ) {
                        SettingsRoute()
                    }
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

private fun navigationFade() =
    fadeIn(tween(AppMotion.NavigationFadeDurationMillis, easing = LinearEasing)) togetherWith
        fadeOut(tween(AppMotion.NavigationFadeDurationMillis, easing = LinearEasing))

/** Keeps an outgoing tab's insets and bottom-bar space intact throughout its transition. */
@Composable
private fun TopLevelDestination(
    selectedItem: AppNavigationItem,
    onItemSelected: (AppNavigationItem) -> Unit,
    content: @Composable () -> Unit,
) {
    val isToday = selectedItem == AppNavigationItem.TODAY
    val isHistory = selectedItem == AppNavigationItem.HISTORY
    val isSettings = selectedItem == AppNavigationItem.SETTINGS
    val useRevisedNavigation = isToday || isHistory || isSettings
    val background = when {
        isToday -> AppHomeTokens.Background
        isHistory -> AppHistoryTokens.Background
        isSettings -> AppSettingsTokens.Background
        else -> AppColors.Background
    }
    val navigationBackground = when {
        isToday || isSettings -> AppHomeTokens.Card
        isHistory -> AppHistoryTokens.Card
        else -> AppColors.Elevated
    }
    Column(modifier = Modifier.fillMaxSize().background(background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(navigationBackground)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
        ) {
            AppBottomNavigation(
                selectedItem = selectedItem,
                onItemSelected = onItemSelected,
                useHomeDesign = useRevisedNavigation,
            )
        }
    }
}
