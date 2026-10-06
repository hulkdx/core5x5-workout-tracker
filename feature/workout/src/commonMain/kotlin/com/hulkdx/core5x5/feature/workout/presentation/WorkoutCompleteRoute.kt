package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutCompleteRoute(workoutId: Long, onBackToToday: () -> Unit) {
    val viewModel: WorkoutCompleteViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutCompleteScreen(
        uiState = uiState,
        onRetryLoad = viewModel::loadWorkout,
        onBackToToday = onBackToToday,
    )
}
