package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ActiveWorkoutRoute(onBack: () -> Unit) {
    val viewModel: ActiveWorkoutViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ActiveWorkoutScreen(uiState = uiState, onBack = onBack)
}
