package com.hulkdx.core5x5.core.preferences.data

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class PersistedTrainingPreferencesRepository(
    private val storage: PreferencesStorage,
) : TrainingPreferencesRepository {
    private val mutex = Mutex()
    private val current = MutableStateFlow<TrainingPreferences?>(null)

    override val preferences: Flow<TrainingPreferences> = flow {
        getPreferences()
        emitAll(current.filterNotNull())
    }

    override suspend fun getPreferences(): TrainingPreferences = mutex.withLock { load() }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        update { it.copy(weightUnit = unit) }
    }

    override suspend fun setRestDurationSeconds(seconds: Long) {
        require(seconds in 1..TrainingPreferences.MAX_REST_DURATION_SECONDS)
        update { it.copy(restDurationSeconds = seconds) }
    }

    private suspend fun update(transform: (TrainingPreferences) -> TrainingPreferences) {
        mutex.withLock {
            val saved = load()
            val next = transform(saved)
            if (next != saved) {
                // Publish only after persistence succeeds. A failed write retains the last saved value.
                withContext(NonCancellable) {
                    storage.write("${next.weightUnit.name}:${next.restDurationSeconds}")
                    current.value = next
                }
            }
        }
    }

    private suspend fun load(): TrainingPreferences {
        current.value?.let { return it }
        val stored = storage.read()?.split(':')
        val unit = WeightUnit.entries.firstOrNull { it.name == stored?.getOrNull(0) } ?: WeightUnit.KG
        val duration = stored?.getOrNull(1)?.toLongOrNull()
            ?.takeIf { it in 1..TrainingPreferences.MAX_REST_DURATION_SECONDS }
            ?: TrainingPreferences.DEFAULT_REST_DURATION_SECONDS
        return TrainingPreferences(unit, duration).also { current.value = it }
    }
}
