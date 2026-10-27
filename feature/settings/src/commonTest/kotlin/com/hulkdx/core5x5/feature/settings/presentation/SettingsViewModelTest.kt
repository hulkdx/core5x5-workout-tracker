package com.hulkdx.core5x5.feature.settings.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
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
    private val permissions = object : RestAlertPermissions {
        override val status = MutableStateFlow(RestAlertPermissionStatus())
        override suspend fun refreshStatus() = Unit
    }
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    @Test
    fun alertSwitchesSaveIndependentlyAndKeepTheSoundChoiceWhileNotificationsAreOff() = runTest(dispatcher) {
        repository.values.value = TrainingPreferences(WeightUnit.LB, 91)
        val viewModel = createLoadedViewModel()
        viewModel.setRestNotificationsEnabled(false)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(WeightUnit.LB, 91, false, true), viewModel.uiState.value.preferences)
        viewModel.setRestNotificationsEnabled(true)
        dispatcher.scheduler.runCurrent()
        viewModel.setRestSoundEnabled(false)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(WeightUnit.LB, 91, true, false), viewModel.uiState.value.preferences)
        assertEquals(3, repository.saves)
    }

    @Test
    fun enablingWhilePermissionIsDeniedSavesPreferenceForLaterGrant() = runTest(dispatcher) {
        repository.values.value = TrainingPreferences(restNotificationsEnabled = false)
        permissions.status.value = RestAlertPermissionStatus(RestNotificationPermission.DENIED)
        val viewModel = createLoadedViewModel()

        viewModel.setRestNotificationsEnabled(true)
        dispatcher.scheduler.runCurrent()

        assertTrue(requireNotNull(viewModel.uiState.value.preferences).restNotificationsEnabled)
        assertEquals(1, repository.saves)
    }

    @Test
    fun switchWritesRetainSavedPositionsAndBlockFurtherEditsUntilCommitted() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        repository.saveGate = CompletableDeferred()
        viewModel.setRestSoundEnabled(false)
        viewModel.setRestNotificationsEnabled(false)
        viewModel.openUnits()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(TrainingPreferences(), viewModel.uiState.value.preferences)
        assertNull(viewModel.uiState.value.dialog)
        assertEquals(1, repository.saves)
        repository.saveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertFalse(requireNotNull(viewModel.uiState.value.preferences).restSoundEnabled)
        assertTrue(requireNotNull(viewModel.uiState.value.preferences).restNotificationsEnabled)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun failedSwitchWritesRetainSavedPositionsAndAllowTheSameActionToRetry() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        repository.saveError = IllegalStateException("disk full")
        viewModel.setRestNotificationsEnabled(false)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(), viewModel.uiState.value.preferences)
        assertEquals(SettingsError.SAVE, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canChangePreferences)
        repository.saveError = null
        viewModel.setRestNotificationsEnabled(false)
        dispatcher.scheduler.runCurrent()
        assertFalse(requireNotNull(viewModel.uiState.value.preferences).restNotificationsEnabled)
        assertTrue(requireNotNull(viewModel.uiState.value.preferences).restSoundEnabled)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun permissionUpdatesPreservePendingSavesAndDenialDoesNotHideOrRewritePreferences() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        repository.saveGate = CompletableDeferred()
        viewModel.openUnits()
        viewModel.selectWeightUnit(WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        permissions.status.value = RestAlertPermissionStatus(RestNotificationPermission.DENIED, false)
        dispatcher.scheduler.runCurrent()
        assertEquals(permissions.status.value, viewModel.uiState.value.restAlertPermissionStatus)
        assertEquals(SettingsDialog.UNITS, viewModel.uiState.value.dialog)
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(TrainingPreferences(), viewModel.uiState.value.preferences)
        repository.saveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingPreferences(WeightUnit.LB), viewModel.uiState.value.preferences)
        assertTrue(viewModel.uiState.value.canChangePreferences)
    }

    @Test
    fun permissionCollectionSurvivesPreferenceLoadFailureAndStopsWithTheOwner() = runTest(dispatcher) {
        repository.loadError = IllegalStateException("read failed")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        permissions.status.value = RestAlertPermissionStatus(RestNotificationPermission.DENIED, true)
        dispatcher.scheduler.runCurrent()
        assertEquals(SettingsError.LOAD, viewModel.uiState.value.error)
        assertEquals(permissions.status.value, viewModel.uiState.value.restAlertPermissionStatus)
        viewModel.setRestSoundEnabled(false)
        viewModel.setRestNotificationsEnabled(false)
        assertEquals(0, repository.saves)
        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, permissions.status.subscriptionCount.value)
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
                single<RestAlertPermissions> { permissions }
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

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        repository = repository,
        appMetadata = metadata,
        restAlertPermissions = permissions,
    ).also { viewModel ->
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
    override suspend fun setRestNotificationsEnabled(enabled: Boolean) = save { it.copy(restNotificationsEnabled = enabled) }
    override suspend fun setRestSoundEnabled(enabled: Boolean) = save { it.copy(restSoundEnabled = enabled) }

    private suspend fun save(transform: (TrainingPreferences) -> TrainingPreferences) {
        saves++
        saveGate?.await()
        saveError?.let { throw it }
        values.value = transform(values.value)
    }
}
