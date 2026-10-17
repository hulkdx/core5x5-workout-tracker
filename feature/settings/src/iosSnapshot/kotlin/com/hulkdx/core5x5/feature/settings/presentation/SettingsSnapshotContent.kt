package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences

@Composable
fun SettingsSnapshotContent() = SettingsScreen(
    uiState = SettingsUiState(isLoading = false, preferences = TrainingPreferences(), versionName = "1.0.0"),
    onOpenUnits = {}, onOpenRestDuration = {}, onOpenAbout = {}, onOpenLicense = {},
    onSelectUnit = {}, onRestDurationInput = {}, onSaveRestDuration = {}, onDismissDialog = {},
    onRetryLoad = {}, onOpenRepository = {}, onOpenPrivacyPolicy = {},
)
