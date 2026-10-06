package com.hulkdx.core5x5.core.preferences.domain

import kotlinx.coroutines.flow.Flow

interface TrainingPreferencesRepository {
    /** Emits persisted defaults/current values followed by successful changes; read failures propagate. */
    val preferences: Flow<TrainingPreferences>

    /** A consistent snapshot for consumers such as creation of the next rest timer. */
    suspend fun getPreferences(): TrainingPreferences

    suspend fun setWeightUnit(unit: WeightUnit)

    suspend fun setRestDurationSeconds(seconds: Long)
}
