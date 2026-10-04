package com.hulkdx.core5x5.shell

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
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ShellRoute(viewModel: ShellViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = remember { mutableStateListOf<ShellDestination>(ShellDestination.Today) }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ShellDestination.Today> {
                ShellScreen(uiState = uiState) {
                    TodayRoute(onWorkoutRequested = { backStack.add(ShellDestination.ActiveWorkout) })
                }
            }
            entry<ShellDestination.ActiveWorkout> {
                ActiveWorkoutRoute(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

private sealed interface ShellDestination {
    data object Today : ShellDestination
    data object ActiveWorkout : ShellDestination
}
