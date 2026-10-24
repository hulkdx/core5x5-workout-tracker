package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRoute(onWorkoutRequested: (Long) -> Boolean, onTabSelected: (TopLevelTab) -> Unit) {
    val viewModel: TodayViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }

    LaunchedEffect(uiState.requestedWorkout) {
        uiState.requestedWorkout?.let { workout ->
            val accepted = onWorkoutRequested(workout.id)
            viewModel.onWorkoutRequestHandled(accepted)
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
