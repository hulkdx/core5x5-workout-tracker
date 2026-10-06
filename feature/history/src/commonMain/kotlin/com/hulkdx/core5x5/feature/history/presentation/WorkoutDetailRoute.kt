package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutDetailRoute(
    workoutId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkoutDetailViewModel = koinViewModel(
        key = "workout-detail:$workoutId",
        parameters = { parametersOf(workoutId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutDetailScreen(uiState = uiState, onBack = onBack, modifier = modifier)
}
