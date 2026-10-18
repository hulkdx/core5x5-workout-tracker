package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRoute(onWorkoutRequested: (Long) -> Unit, onTabSelected: (TopLevelTab) -> Unit) {
    val viewModel: TodayViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadWorkout()
    }

    LaunchedEffect(uiState.requestedWorkout) {
        uiState.requestedWorkout?.let { workout ->
            viewModel.onWorkoutRequestHandled()
            onWorkoutRequested(workout.id)
        }
    }

    TopLevelScreenLayout(selectedTab = TopLevelTab.TODAY, onTabSelected = onTabSelected) {
        TodayScreen(
            uiState = uiState,
            weightUnit = uiState.weightUnit,
            onRetryPreferences = viewModel::retryPreferences,
            onStartWorkout = viewModel::startWorkout,
            onResumeWorkout = viewModel::requestResume,
            onRetryLoad = viewModel::loadWorkout,
            onSwitchWorkout = viewModel::switchWorkout,
        )
    }
}
