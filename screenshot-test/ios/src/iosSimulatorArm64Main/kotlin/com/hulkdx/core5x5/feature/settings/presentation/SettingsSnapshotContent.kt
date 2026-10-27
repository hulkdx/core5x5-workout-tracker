package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit

@Composable
fun SettingsSnapshotContent(scene: String) = SettingsScreen(
    uiState = SettingsUiState(
        isLoading = false,
        preferences = TrainingPreferences(
            if (scene == "settingsUnits" || scene == "settingsUnitsError") WeightUnit.LB else WeightUnit.KG,
        ),
        versionName = "1.0.0",
        dialog = when (scene) {
            "settingsUnits", "settingsUnitsError" -> SettingsDialog.UNITS
            "settingsAbout", "settingsAboutError" -> SettingsDialog.ABOUT
            "settingsLicense" -> SettingsDialog.LICENSE
            else -> null
        },
        error = when (scene) {
            "settingsUnitsError" -> SettingsError.SAVE
            "settingsAboutError" -> SettingsError.OPEN_REPOSITORY
            else -> null
        },
    ),
    onOpenUnits = {}, onOpenAbout = {}, onOpenLicense = {},
    onSelectUnit = {}, onDismissDialog = {},
    onRetryLoad = {}, onOpenRepository = {}, onOpenPrivacyPolicy = {},
    onOpenPreciseTimingSettings = {},
)
