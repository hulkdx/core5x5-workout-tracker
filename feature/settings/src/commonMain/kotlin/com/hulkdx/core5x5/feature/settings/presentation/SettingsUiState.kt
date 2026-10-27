package com.hulkdx.core5x5.feature.settings.presentation

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: TrainingPreferences? = null,
    val versionName: String? = null,
    val dialog: SettingsDialog? = null,
    val isSaving: Boolean = false,
    val error: SettingsError? = null,
    val restAlertPermissionStatus: RestAlertPermissionStatus = RestAlertPermissionStatus(),
) {
    val canChangePreferences: Boolean get() = preferences != null && !isLoading && !isSaving
    val restNotificationsEnabled: Boolean
        get() = (preferences?.restNotificationsEnabled ?: true) &&
            restAlertPermissionStatus.notificationPermission == RestNotificationPermission.GRANTED
    val canChangeRestSound: Boolean get() = canChangePreferences && restNotificationsEnabled
}

enum class SettingsDialog { UNITS, ABOUT, LICENSE }
enum class SettingsError { LOAD, SAVE, OPEN_REPOSITORY, OPEN_PRIVACY_POLICY }
