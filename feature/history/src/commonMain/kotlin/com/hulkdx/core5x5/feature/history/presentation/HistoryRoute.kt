package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryRoute(
    onWorkoutSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        onWorkoutSelected = onWorkoutSelected,
        modifier = modifier,
    )
}
