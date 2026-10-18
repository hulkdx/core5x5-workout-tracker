package com.hulkdx.core5x5.feature.settings.presentation

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: TrainingPreferences? = null,
    val versionName: String? = null,
    val dialog: SettingsDialog? = null,
    val isSaving: Boolean = false,
    val error: SettingsError? = null,
) {
    val canChangePreferences: Boolean get() = preferences != null && !isLoading && !isSaving
}

enum class SettingsDialog { UNITS, ABOUT, LICENSE }
enum class SettingsError { LOAD, SAVE, OPEN_REPOSITORY, OPEN_PRIVACY_POLICY }
