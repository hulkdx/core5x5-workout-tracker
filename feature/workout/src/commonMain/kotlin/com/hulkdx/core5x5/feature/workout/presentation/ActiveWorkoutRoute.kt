package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActiveWorkoutRoute(workoutId: Long, onBack: () -> Unit, onWorkoutCompleted: (Long) -> Unit) {
    val viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.requestedCompletedWorkoutId) {
        uiState.requestedCompletedWorkoutId?.let { workoutId ->
            viewModel.onCompletionRequestHandled()
            onWorkoutCompleted(workoutId)
        }
    }

    ActiveWorkoutScreen(
        uiState = uiState,
        onBack = onBack,
        onFinishWorkout = viewModel::finishWorkout,
        onRetryLoad = viewModel::loadWorkout,
    )
}
