package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutCompleteRoute(workoutId: Long, onBackToToday: () -> Unit, weightUnit: WeightUnit = WeightUnit.KG) {
    val viewModel: WorkoutCompleteViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutCompleteScreen(
        uiState = uiState,
        weightUnit = weightUnit,
        onRetryLoad = viewModel::loadWorkout,
        onBackToToday = onBackToToday,
    )
}
