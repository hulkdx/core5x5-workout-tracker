package com.hulkdx.core5x5.feature.settings.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.settings.di.settingsModule
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.resolveViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakePreferencesRepository()
    private val metadata = AppMetadata { "2.7.1" }
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    @Test
    fun loadShowsSavedPreferencesAndRealMetadataWithoutEnablingEarlyChanges() = runTest(dispatcher) {
        repository.values.value = TrainingPreferences(WeightUnit.LB, 91)
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.openUnits()
        assertNull(viewModel.uiState.value.dialog)
        dispatcher.scheduler.runCurrent()

        assertEquals(repository.values.value, viewModel.uiState.value.preferences)
        assertEquals("2.7.1", viewModel.uiState.value.versionName)
        assertTrue(viewModel.uiState.value.canChangePreferences)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun changingUnitsKeepsRestDurationAndClosesOnlyAfterSavedSuccess() = runTest(dispatcher) {
        repository.values.value = TrainingPreferences(restDurationSeconds = 91)
        val viewModel = createLoadedViewModel()
        repository.saveGate = CompletableDeferred()
        viewModel.openUnits()
        viewModel.selectWeightUnit(WeightUnit.LB)
        viewModel.selectWeightUnit(WeightUnit.KG)
        viewModel.dismissDialog()
        viewModel.openAbout()
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(SettingsDialog.UNITS, viewModel.uiState.value.dialog)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.saves)
        assertEquals(WeightUnit.KG, viewModel.uiState.value.preferences?.weightUnit)

        repository.saveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(WeightUnit.LB, 91), viewModel.uiState.value.preferences)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun pendingUnitChangeKeepsObservingExternalPreferenceUpdates() = runTest(dispatcher) {
        repository.values.value = TrainingPreferences(restDurationSeconds = 91)
        val viewModel = createLoadedViewModel()
        repository.saveGate = CompletableDeferred()
        viewModel.openUnits()
        viewModel.selectWeightUnit(WeightUnit.LB)
        dispatcher.scheduler.runCurrent()

        repository.values.value = TrainingPreferences(restDurationSeconds = 42)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(restDurationSeconds = 42), viewModel.uiState.value.preferences)
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(SettingsDialog.UNITS, viewModel.uiState.value.dialog)

        repository.saveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(WeightUnit.LB, 42), repository.values.value)
        assertEquals(repository.values.value, viewModel.uiState.value.preferences)
        assertEquals(1, repository.saves)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun saveFailureKeepsTheSavedValueAndDialogForRetry() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        repository.saveError = IllegalStateException("disk unavailable")
        viewModel.openUnits()
        viewModel.selectWeightUnit(WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        assertEquals(WeightUnit.KG, viewModel.uiState.value.preferences?.weightUnit)
        assertEquals(SettingsError.SAVE, viewModel.uiState.value.error)
        assertEquals(SettingsDialog.UNITS, viewModel.uiState.value.dialog)
        assertFalse(viewModel.uiState.value.isSaving)

        repository.saveError = null
        viewModel.selectWeightUnit(WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        assertEquals(WeightUnit.LB, viewModel.uiState.value.preferences?.weightUnit)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun loadFailureIsRetryableWithoutDisplayingUnverifiedDefaults() = runTest(dispatcher) {
        repository.loadError = IllegalStateException("cannot read")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(SettingsError.LOAD, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.preferences)
        assertFalse(viewModel.uiState.value.canChangePreferences)

        repository.loadError = null
        repository.values.value = TrainingPreferences(WeightUnit.LB, 42)
        viewModel.loadPreferences()
        dispatcher.scheduler.runCurrent()
        assertEquals(repository.values.value, viewModel.uiState.value.preferences)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun externalChangesUpdateTheScreenAndOwnerCleanupStopsObservation() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        assertEquals(1, repository.values.subscriptionCount.value)
        repository.values.value = TrainingPreferences(WeightUnit.LB, 42)
        dispatcher.scheduler.runCurrent()
        assertEquals(repository.values.value, viewModel.uiState.value.preferences)

        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, repository.values.subscriptionCount.value)
    }

    @Test
    fun coroutineCancellationIsNotReportedAsAnOperationFailure() = runTest(dispatcher) {
        repository.loadError = CancellationException("owner cleared")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.preferences)
    }

    @Test
    fun aboutLicenseAndRepositoryErrorsDoNotMutatePreferences() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        viewModel.openAbout()
        viewModel.onRepositoryOpenFailed()
        assertEquals(SettingsError.OPEN_REPOSITORY, viewModel.uiState.value.error)
        viewModel.openLicense()
        assertEquals(SettingsDialog.LICENSE, viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.error)
        viewModel.openAbout()
        viewModel.dismissDialog()
        assertEquals(TrainingPreferences(), repository.values.value)
        assertEquals(0, repository.saves)
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun settingsGraphInjectsOnePreferenceSourceAndKeepsViewModelsDestinationScoped() = runTest(dispatcher) {
        val application = koinApplication {
            modules(settingsModule, module {
                single<TrainingPreferencesRepository> { repository }
                single<AppMetadata> { metadata }
            })
        }
        fun resolve(store: ViewModelStore): SettingsViewModel = resolveViewModel(
            vmClass = SettingsViewModel::class,
            viewModelStore = store,
            extras = CreationExtras.Empty,
            scope = application.koin.scopeRegistry.rootScope,
        )
        try {
            val firstStore = ViewModelStore().also(stores::add)
            val first = resolve(firstStore)
            val second = resolve(ViewModelStore().also(stores::add))
            assertSame(first, resolve(firstStore))
            assertNotSame(first, second)
            dispatcher.scheduler.runCurrent()
            repository.values.value = TrainingPreferences(WeightUnit.LB)
            dispatcher.scheduler.runCurrent()
            assertEquals(WeightUnit.LB, first.uiState.value.preferences?.weightUnit)
            assertEquals(first.uiState.value.preferences, second.uiState.value.preferences)
        } finally {
            stores.forEach { it.clear() }
            application.close()
        }
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(repository, metadata).also { viewModel ->
        ViewModelStore().also { it.put("settings", viewModel); stores.add(it) }
    }

    private fun createLoadedViewModel(): SettingsViewModel = createViewModel().also { dispatcher.scheduler.runCurrent() }
}

private class FakePreferencesRepository : TrainingPreferencesRepository {
    val values = MutableStateFlow(TrainingPreferences())
    var loadError: Exception? = null
    var saveError: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var saves = 0
    override val preferences = flow {
        loadError?.let { throw it }
        emitAll(values)
    }

    override suspend fun getPreferences(): TrainingPreferences = values.value
    override suspend fun setWeightUnit(unit: WeightUnit) = save { it.copy(weightUnit = unit) }
    override suspend fun setRestDurationSeconds(seconds: Long) = error("Settings only changes the weight unit")

    private suspend fun save(transform: (TrainingPreferences) -> TrainingPreferences) {
        saves++
        saveGate?.await()
        saveError?.let { throw it }
        values.value = transform(values.value)
    }
}
