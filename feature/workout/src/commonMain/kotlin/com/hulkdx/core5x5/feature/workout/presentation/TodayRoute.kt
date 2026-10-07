package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRoute(weightUnit: WeightUnit = WeightUnit.KG, onWorkoutRequested: (Long) -> Unit) {
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

    TodayScreen(
        uiState = uiState,
        weightUnit = weightUnit,
        onStartWorkout = viewModel::startWorkout,
        onResumeWorkout = viewModel::requestResume,
        onRetryLoad = viewModel::loadWorkout,
        onSwitchWorkout = viewModel::switchWorkout,
    )
}
