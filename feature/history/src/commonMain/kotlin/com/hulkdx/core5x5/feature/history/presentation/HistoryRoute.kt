package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryRoute(
    onTabSelected: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadHistory()
    }

    TopLevelScreenLayout(selectedTab = TopLevelTab.HISTORY, onTabSelected = onTabSelected, modifier = modifier) {
        HistoryScreen(
            uiState = uiState,
            onRetryLoad = viewModel::loadHistory,
            onExerciseSelected = viewModel::selectExercise,
            weightUnit = uiState.weightUnit,
            onRetryPreferences = viewModel::retryPreferences,
        )
    }
}
