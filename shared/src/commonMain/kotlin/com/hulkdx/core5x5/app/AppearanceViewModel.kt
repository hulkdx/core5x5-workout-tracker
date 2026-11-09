package com.hulkdx.core5x5.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.preferences.domain.Appearance
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns only app appearance; destination state and navigation remain with their existing owners. */
class AppearanceViewModel(private val repository: TrainingPreferencesRepository) : ViewModel() {
    private val savedAppearance = MutableStateFlow<Appearance?>(null)
    val appearance = savedAppearance.asStateFlow()
    private var observation: Job? = null

    init {
        refresh()
    }

    /** Resume retries a failed read without duplicating a healthy subscription. */
    fun refresh() {
        if (observation?.isActive == true) return
        observation = viewModelScope.launch {
            try {
                repository.preferences.collect { savedAppearance.value = it.appearance }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (savedAppearance.value == null) savedAppearance.value = Appearance.SYSTEM
                Logger.warn("Appearance", "Could not read appearance preferences", error)
            }
        }
    }
}
