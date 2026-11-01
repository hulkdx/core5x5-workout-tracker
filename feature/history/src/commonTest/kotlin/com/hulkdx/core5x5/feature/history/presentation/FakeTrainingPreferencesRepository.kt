package com.hulkdx.core5x5.feature.history.presentation

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FakeTrainingPreferencesRepository : TrainingPreferencesRepository {
    val values = MutableStateFlow(TrainingPreferences())
    var loadError: Exception? = null
    var observations = 0
    override val preferences = flow {
        observations++
        loadError?.let { throw it }
        emitAll(values)
    }
    override suspend fun getPreferences() = values.value
    override suspend fun setWeightUnit(unit: WeightUnit) { values.value = values.value.copy(weightUnit = unit) }
    override suspend fun setRestDurationSeconds(seconds: Long) {
        values.value = values.value.copy(restDurationSeconds = seconds)
    }
    override suspend fun setRestNotificationsEnabled(enabled: Boolean) {
        values.value = values.value.copy(restNotificationsEnabled = enabled)
    }
    override suspend fun setRestSoundEnabled(enabled: Boolean) {
        values.value = values.value.copy(restSoundEnabled = enabled)
    }
}
