package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** Focused nested destination; its ViewModel retains selected lift across refreshes. */
@Composable
fun HistoryGraphRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: HistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadHistory()
    }

    HistoryGraphScreen(
        uiState = uiState,
        onBack = onBack,
        onRetryLoad = viewModel::loadHistory,
        onExerciseSelected = viewModel::selectExercise,
        onRetryPreferences = viewModel::retryPreferences,
        modifier = modifier,
    )
}
