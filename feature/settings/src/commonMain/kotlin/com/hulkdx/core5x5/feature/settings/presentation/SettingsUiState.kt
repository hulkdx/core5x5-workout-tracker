package com.hulkdx.core5x5.feature.settings.presentation

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.feature.settings.domain.parseRestDurationSeconds

internal data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: TrainingPreferences? = null,
    val versionName: String? = null,
    val dialog: SettingsDialog? = null,
    val restDurationInput: String = "",
    val hasInvalidRestDuration: Boolean = false,
    val isSaving: Boolean = false,
    val error: SettingsError? = null,
) {
    val canChangePreferences: Boolean get() = preferences != null && !isLoading && !isSaving
    val canSaveRestDuration: Boolean
        get() = canChangePreferences && parseRestDurationSeconds(restDurationInput) != null
}

internal enum class SettingsDialog { UNITS, REST_DURATION, ABOUT, LICENSE }
internal enum class SettingsError { LOAD, SAVE, OPEN_REPOSITORY }
