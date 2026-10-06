package com.hulkdx.core5x5.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import com.hulkdx.core5x5.feature.settings.domain.parseRestDurationSeconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val repository: TrainingPreferencesRepository,
    appMetadata: AppMetadata,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(versionName = runCatching { appMetadata.versionName() }.getOrNull()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        loadPreferences()
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

    fun openRestDuration() {
        if (!uiState.value.canChangePreferences) return
        _uiState.value = uiState.value.copy(
            dialog = SettingsDialog.REST_DURATION,
            restDurationInput = requireNotNull(uiState.value.preferences).restDurationSeconds.toString(),
            hasInvalidRestDuration = false,
            error = null,
        )
    }

    fun setRestDurationInput(input: String) {
        if (uiState.value.isSaving || uiState.value.dialog != SettingsDialog.REST_DURATION) return
        _uiState.value = uiState.value.copy(
            restDurationInput = input,
            hasInvalidRestDuration = parseRestDurationSeconds(input) == null,
            error = null,
        )
    }

    fun selectWeightUnit(unit: WeightUnit) {
        if (uiState.value.dialog != SettingsDialog.UNITS) return
        save { repository.setWeightUnit(unit) }
    }

    fun saveRestDuration() {
        if (uiState.value.dialog != SettingsDialog.REST_DURATION) return
        val seconds = parseRestDurationSeconds(uiState.value.restDurationInput)
        if (seconds == null) {
            _uiState.value = uiState.value.copy(hasInvalidRestDuration = true)
            return
        }
        save { repository.setRestDurationSeconds(seconds) }
    }

    fun openAbout() = showDialog(SettingsDialog.ABOUT)
    fun openLicense() = showDialog(SettingsDialog.LICENSE)

    fun onRepositoryOpenFailed() {
        _uiState.value = uiState.value.copy(error = SettingsError.OPEN_REPOSITORY)
    }

    fun dismissDialog() {
        if (uiState.value.isSaving) return
        _uiState.value = uiState.value.copy(dialog = null, error = null, hasInvalidRestDuration = false)
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
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(error = SettingsError.SAVE)
            } finally {
                _uiState.value = uiState.value.copy(isSaving = false)
            }
        }
    }
}
