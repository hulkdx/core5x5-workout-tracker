package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutRoute
import com.hulkdx.core5x5.feature.workout.presentation.TodayRoute
import com.hulkdx.core5x5.shell.ShellScreen
import com.hulkdx.core5x5.shell.ShellViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AppNavigation(viewModel: ShellViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Today) }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.Today> {
                ShellScreen(uiState = uiState) {
                    TodayRoute(onWorkoutRequested = { backStack.add(AppDestination.ActiveWorkout) })
                }
            }
            entry<AppDestination.ActiveWorkout> {
                ActiveWorkoutRoute(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

private sealed interface AppDestination {
    data object Today : AppDestination
    data object ActiveWorkout : AppDestination
}
