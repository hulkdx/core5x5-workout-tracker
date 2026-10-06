package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

// Embed in Active Workout under its destination owner when rest timing is implemented.
@Composable
fun RestTimerRoute(modifier: Modifier = Modifier) {
    val viewModel: RestTimerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RestTimerScreen(uiState = uiState, modifier = modifier)
}
