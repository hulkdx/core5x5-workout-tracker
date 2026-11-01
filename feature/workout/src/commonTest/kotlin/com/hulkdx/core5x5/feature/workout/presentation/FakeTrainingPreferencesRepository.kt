package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FakeTrainingPreferencesRepository(
    var value: TrainingPreferences = TrainingPreferences(),
) : TrainingPreferencesRepository {
    val values = MutableStateFlow(value)
    var readError: Exception? = null

    var loadError: Exception? = null
    var observations = 0
    override val preferences = flow {
        observations++
        loadError?.let { throw it }
        emitAll(values)
    }

    override suspend fun getPreferences(): TrainingPreferences {
        readError?.let { throw it }
        return value
    }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        value = value.copy(weightUnit = unit)
        values.value = value
    }

    override suspend fun setRestDurationSeconds(seconds: Long) {
        value = value.copy(restDurationSeconds = seconds)
        values.value = value
    }

    override suspend fun setRestNotificationsEnabled(enabled: Boolean) {
        value = value.copy(restNotificationsEnabled = enabled)
        values.value = value
    }

    override suspend fun setRestSoundEnabled(enabled: Boolean) {
        value = value.copy(restSoundEnabled = enabled)
        values.value = value
    }
}
