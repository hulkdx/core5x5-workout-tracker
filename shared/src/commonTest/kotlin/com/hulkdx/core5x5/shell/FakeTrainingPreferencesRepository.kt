package com.hulkdx.core5x5.shell

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class FakeTrainingPreferencesRepository : TrainingPreferencesRepository {
    val values = MutableStateFlow(TrainingPreferences())
    var loadError: Exception? = null

    override val preferences = flow {
        loadError?.let { throw it }
        emitAll(values)
    }
    override suspend fun getPreferences(): TrainingPreferences = values.value
    override suspend fun setWeightUnit(unit: WeightUnit) {
        values.value = values.value.copy(weightUnit = unit)
    }
    override suspend fun setRestDurationSeconds(seconds: Long) {
        values.value = values.value.copy(restDurationSeconds = seconds)
    }
}
