package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryRoute(
    onWorkoutSelected: (Long) -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadHistory()
    }

    HistoryScreen(
        uiState = uiState,
        onWorkoutSelected = onWorkoutSelected,
        onRetryLoad = viewModel::loadHistory,
        weightUnit = weightUnit,
        modifier = modifier,
    )
}
