package com.hulkdx.core5x5.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TrainingPreferencesRepository,
    appMetadata: AppMetadata,
    restAlertPermissions: RestAlertPermissions,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = runCatching { appMetadata.versionName() }.getOrNull(),
            restAlertPermissionStatus = restAlertPermissions.status.value,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        loadPreferences()
        viewModelScope.launch {
            restAlertPermissions.status.collect { status ->
                _uiState.value = uiState.value.copy(restAlertPermissionStatus = status)
            }
        }
    }

    fun loadPreferences() {
        if (uiState.value.isSaving) return
        observation?.cancel()
        _uiState.value = uiState.value.copy(isLoading = true, error = null)
        observation = viewModelScope.launch {
            try {
                repository.preferences.collect { preferences ->
                    _uiState.value = uiState.value.copy(isLoading = false, preferences = preferences, error = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, error = SettingsError.LOAD)
            }
        }
    }

    fun openUnits() {
        if (uiState.value.canChangePreferences) showDialog(SettingsDialog.UNITS)
    }

    fun selectWeightUnit(unit: WeightUnit) {
        if (uiState.value.dialog != SettingsDialog.UNITS) return
        save { repository.setWeightUnit(unit) }
    }

    fun setRestNotificationsEnabled(enabled: Boolean) {
        save { repository.setRestNotificationsEnabled(enabled) }
    }

    fun setRestSoundEnabled(enabled: Boolean) {
        save { repository.setRestSoundEnabled(enabled) }
    }

    fun openAbout() = showDialog(SettingsDialog.ABOUT)
    fun openLicense() = showDialog(SettingsDialog.LICENSE)

    fun onRepositoryOpenFailed() {
        _uiState.value = uiState.value.copy(error = SettingsError.OPEN_REPOSITORY)
    }

    fun onPrivacyPolicyOpenFailed() {
        _uiState.value = uiState.value.copy(error = SettingsError.OPEN_PRIVACY_POLICY)
    }

    fun onPrivacyPolicyOpened() {
        if (uiState.value.error == SettingsError.OPEN_PRIVACY_POLICY) {
            _uiState.value = uiState.value.copy(error = null)
        }
    }

    fun dismissDialog() {
        if (uiState.value.isSaving) return
        _uiState.value = uiState.value.copy(dialog = null, error = null)
    }

    private fun showDialog(dialog: SettingsDialog) {
        if (!uiState.value.isSaving) _uiState.value = uiState.value.copy(dialog = dialog, error = null)
    }

    private fun save(operation: suspend () -> Unit) {
        if (!uiState.value.canChangePreferences) return
        _uiState.value = uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                operation()
                _uiState.value = uiState.value.copy(dialog = null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = uiState.value.copy(error = SettingsError.SAVE)
                Logger.warn("Settings", "Could not save preferences", error)
            } finally {
                _uiState.value = uiState.value.copy(isSaving = false)
            }
        }
    }
}
