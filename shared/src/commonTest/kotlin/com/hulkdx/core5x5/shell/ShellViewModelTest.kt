package com.hulkdx.core5x5.shell

import androidx.lifecycle.ViewModelStore
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ShellViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeTrainingPreferencesRepository()
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    @Test
    fun shellObservesUnitChangesForWorkoutDestinations() = runTest(dispatcher) {
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(WeightUnit.KG, viewModel.uiState.value.weightUnit)
        repository.setWeightUnit(WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        assertEquals(WeightUnit.LB, viewModel.uiState.value.weightUnit)
        repository.setWeightUnit(WeightUnit.KG)
        dispatcher.scheduler.runCurrent()
        assertEquals(WeightUnit.KG, viewModel.uiState.value.weightUnit)
    }

    @Test
    fun failedPreferenceReadsKeepExplicitUnitsAndCanBeRetried() = runTest(dispatcher) {
        repository.loadError = IllegalStateException("cannot load preferences")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasPreferencesError)
        assertEquals(WeightUnit.KG, viewModel.uiState.value.weightUnit)
        repository.loadError = null
        repository.setWeightUnit(WeightUnit.LB)
        viewModel.loadPreferences()
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.hasPreferencesError)
        assertEquals(WeightUnit.LB, viewModel.uiState.value.weightUnit)
    }

    @Test
    fun ownerCleanupStopsObservingSharedPreferences() = runTest(dispatcher) {
        createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.values.subscriptionCount.value)
        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, repository.values.subscriptionCount.value)
    }

    private fun createViewModel(): ShellViewModel = ShellViewModel(repository).also { viewModel ->
        ViewModelStore().also { it.put("shell", viewModel); stores.add(it) }
    }
}
