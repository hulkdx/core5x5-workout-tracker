package com.hulkdx.core5x5.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.feature.workout.presentation.TodayRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ShellRoute(viewModel: ShellViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShellScreen(uiState = uiState) {
        TodayRoute()
    }
}
