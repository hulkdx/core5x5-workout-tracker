package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutDetailRoute(
    workoutId: Long,
    onBack: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkoutDetailViewModel = koinViewModel(
        key = "workout-detail:$workoutId",
        parameters = { parametersOf(workoutId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetryLoad = viewModel::loadWorkout,
        weightUnit = weightUnit,
        modifier = modifier,
    )
}
