package com.hulkdx.core5x5.core.preferences.data

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersistedTrainingPreferencesRepositoryTest {
    @Test
    fun absentPreferencesLoadKgAndThreeMinutesWithoutCreatingStorage() = runTest {
        val storage = MemoryPreferencesStorage()
        val repository = PersistedTrainingPreferencesRepository(storage)

        assertEquals(TrainingPreferences(), repository.preferences.first())
        assertEquals(180_000L, repository.getPreferences().restDurationMillis)
        assertEquals(TrainingPreferences(), PersistedTrainingPreferencesRepository(storage).getPreferences())
        assertNull(storage.value)
        assertEquals(0, storage.writes)
    }

    @Test
    fun updatesAreObservableAndSurviveRepositoryRecreation() = runTest {
        val storage = MemoryPreferencesStorage()
        val repository = PersistedTrainingPreferencesRepository(storage)
        val observed = mutableListOf<TrainingPreferences>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.preferences.collect(observed::add) }

        repository.setWeightUnit(WeightUnit.LB)
        repository.setRestDurationSeconds(91)

        assertEquals(
            listOf(TrainingPreferences(), TrainingPreferences(WeightUnit.LB), TrainingPreferences(WeightUnit.LB, 91)),
            observed,
        )
        assertEquals(TrainingPreferences(WeightUnit.LB, 91), PersistedTrainingPreferencesRepository(storage).preferences.first())
        assertEquals("LB:91", storage.value)
    }

    @Test
    fun concurrentFieldUpdatesDoNotOverwriteEachOther() = runTest {
        val storage = MemoryPreferencesStorage()
        val repository = PersistedTrainingPreferencesRepository(storage)
        val unit = async { repository.setWeightUnit(WeightUnit.LB) }
        val duration = async { repository.setRestDurationSeconds(3_601) }
        unit.await()
        duration.await()

        assertEquals(TrainingPreferences(WeightUnit.LB, 3_601), repository.getPreferences())
        assertEquals(1, storage.reads)
        assertEquals(repository.getPreferences(), PersistedTrainingPreferencesRepository(storage).getPreferences())
        assertEquals(2, storage.reads)
    }

    @Test
    fun invalidStoredFieldsFallBackIndependentlyAndDoNotWrite() = runTest {
        for ((raw, expected) in listOf(
            "unknown:90" to TrainingPreferences(restDurationSeconds = 90),
            "LB:0" to TrainingPreferences(WeightUnit.LB),
            "LB:-12" to TrainingPreferences(WeightUnit.LB),
            "LB:9223372036854775807" to TrainingPreferences(WeightUnit.LB),
            "broken" to TrainingPreferences(),
        )) {
            val storage = MemoryPreferencesStorage(raw)
            assertEquals(expected, PersistedTrainingPreferencesRepository(storage).getPreferences())
            assertEquals(0, storage.writes)
            assertEquals(raw, storage.value)
        }
    }

    @Test
    fun readFailuresPropagateAndCanBeRetriedWithoutCachingFalseDefaults() = runTest {
        val storage = MemoryPreferencesStorage("LB:45").apply { failRead = true }
        val repository = PersistedTrainingPreferencesRepository(storage)
        assertFailsWith<IllegalStateException> { repository.preferences.first() }

        storage.failRead = false
        assertEquals(TrainingPreferences(WeightUnit.LB, 45), repository.preferences.first())
    }

    @Test
    fun failedWritesRetainSavedAndObservedValuesAndSupportRetry() = runTest {
        val storage = MemoryPreferencesStorage("KG:90")
        val repository = PersistedTrainingPreferencesRepository(storage)
        val saved = repository.getPreferences()
        storage.failWrite = true
        assertFailsWith<IllegalStateException> { repository.setWeightUnit(WeightUnit.LB) }
        assertEquals(saved, repository.preferences.first())
        assertEquals(saved, PersistedTrainingPreferencesRepository(storage).getPreferences())

        storage.failWrite = false
        repository.setWeightUnit(WeightUnit.LB)
        assertEquals(TrainingPreferences(WeightUnit.LB, 90), repository.getPreferences())
    }

    @Test
    fun cancellationDuringAnAlreadyStartedWriteDoesNotLeaveTheObservableValueStale() = runTest {
        val storage = MemoryPreferencesStorage().apply { writeGate = CompletableDeferred() }
        val repository = PersistedTrainingPreferencesRepository(storage)
        val saving = launch { repository.setWeightUnit(WeightUnit.LB) }
        runCurrent()
        assertTrue(storage.writeStarted)
        saving.cancel()
        storage.writeGate?.complete(Unit)
        saving.join()

        assertEquals(WeightUnit.LB, repository.getPreferences().weightUnit)
        assertEquals(repository.getPreferences(), PersistedTrainingPreferencesRepository(storage).getPreferences())
    }

    @Test
    fun positiveSecondsHaveNoArtificialOneHourCapAndOldSnapshotsStayUnchanged() = runTest {
        val repository = PersistedTrainingPreferencesRepository(MemoryPreferencesStorage())
        val timerCreationSnapshot = repository.getPreferences()
        for (seconds in listOf(1L, 3_601L, 86_400L, TrainingPreferences.MAX_REST_DURATION_SECONDS)) {
            repository.setRestDurationSeconds(seconds)
            assertEquals(seconds * 1_000L, repository.getPreferences().restDurationMillis)
        }
        assertEquals(180_000L, timerCreationSnapshot.restDurationMillis)
        for (seconds in listOf(0L, -1L, TrainingPreferences.MAX_REST_DURATION_SECONDS + 1, Long.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { repository.setRestDurationSeconds(seconds) }
        }
    }

    @Test
    fun selectingAnAlreadySavedValueAvoidsAnUnnecessaryWrite() = runTest {
        val storage = MemoryPreferencesStorage("LB:90")
        val repository = PersistedTrainingPreferencesRepository(storage)
        repository.setWeightUnit(WeightUnit.LB)
        repository.setRestDurationSeconds(90)
        assertEquals(0, storage.writes)
    }
}

private class MemoryPreferencesStorage(var value: String? = null) : PreferencesStorage {
    var reads = 0
    var writes = 0
    var failRead = false
    var failWrite = false
    var writeStarted = false
    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun read(): String? {
        reads++
        check(!failRead)
        return value
    }

    override suspend fun write(value: String) {
        writeStarted = true
        writeGate?.await()
        check(!failWrite)
        this.value = value
        writes++
    }
}
