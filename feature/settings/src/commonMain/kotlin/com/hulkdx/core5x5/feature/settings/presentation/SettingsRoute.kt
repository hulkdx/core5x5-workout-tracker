package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    SettingsScreen(
        uiState = uiState,
        modifier = modifier,
        onOpenUnits = viewModel::openUnits,
        onOpenRestDuration = viewModel::openRestDuration,
        onOpenAbout = viewModel::openAbout,
        onOpenLicense = viewModel::openLicense,
        onSelectUnit = viewModel::selectWeightUnit,
        onRestDurationInput = viewModel::setRestDurationInput,
        onSaveRestDuration = viewModel::saveRestDuration,
        onDismissDialog = viewModel::dismissDialog,
        onRetryLoad = viewModel::loadPreferences,
        onOpenRepository = {
            try {
                uriHandler.openUri("https://github.com/hulkdx/core5x5-workout-tracker")
            } catch (_: Exception) {
                viewModel.onRepositoryOpenFailed()
            }
        },
    )
}
