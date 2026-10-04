package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRoute(onWorkoutRequested: () -> Unit) {
    val viewModel: TodayViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.requestedWorkout) {
        if (uiState.requestedWorkout != null) {
            viewModel.onWorkoutRequestHandled()
            onWorkoutRequested()
        }
    }

    TodayScreen(
        uiState = uiState,
        onStartWorkout = viewModel::startWorkout,
        onResumeWorkout = viewModel::requestResume,
        onRetryLoad = viewModel::loadWorkout,
    )
}
