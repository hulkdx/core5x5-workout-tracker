package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryRoute(
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    val viewModel: HistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadHistory()
    }

    HistoryScreen(
        uiState = uiState,
        onRetryLoad = viewModel::loadHistory,
        onExerciseSelected = viewModel::selectExercise,
        weightUnit = weightUnit,
        modifier = modifier,
    )
}
