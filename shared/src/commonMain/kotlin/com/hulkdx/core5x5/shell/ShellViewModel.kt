package com.hulkdx.core5x5.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ShellViewModel(private val preferences: TrainingPreferencesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                preferences.preferences.collect { preferences ->
                    _uiState.value = uiState.value.copy(weightUnit = preferences.weightUnit, hasPreferencesError = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(hasPreferencesError = true)
            }
        }
    }
}
