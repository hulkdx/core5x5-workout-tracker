package com.hulkdx.core5x5.di

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.shell.FakeTrainingPreferencesRepository
import com.hulkdx.core5x5.shell.ShellViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.resolveViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppCompositionTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var application: KoinApplication
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = newApplication()
    }

    @AfterTest
    fun tearDown() {
        stores.forEach { it.clear() }
        application.close()
        Dispatchers.resetMain()
    }

    @Test
    fun theSameOwnerRetainsItsViewModel() {
        val store = newStore()

        assertSame(resolve(store), resolve(store))
    }

    @Test
    fun differentOwnersGetDifferentViewModels() {
        assertNotSame(resolve(newStore()), resolve(newStore()))
    }

    @Test
    fun clearingAnOwnerCancelsItsWorkAndReleasesItsViewModel() = runTest(dispatcher) {
        val store = newStore()
        val original = resolve(store)
        val job = original.viewModelScope.launch { awaitCancellation() }
        dispatcher.scheduler.runCurrent()
        assertTrue(job.isActive)

        store.clear()

        assertTrue(job.isCancelled)
        assertNotSame(original, resolve(store))
    }

    @Test
    fun aFreshAppGraphHasNoViewModelFromAnEarlierGraph() {
        val original = resolve(newStore())
        val nextApplication = newApplication()
        val nextStore = ViewModelStore()
        try {
            assertNotSame(original, resolve(nextStore, nextApplication.koin))
        } finally {
            nextStore.clear()
            nextApplication.close()
        }
    }

    private fun newStore(): ViewModelStore = ViewModelStore().also(stores::add)

    private fun newApplication() = koinApplication {
        modules(appModule, module {
            single<TrainingPreferencesRepository> { FakeTrainingPreferencesRepository() }
        })
    }

    // Exercise the resolver used by koinViewModel without a Compose UI test harness.
    // Keep this pinned-version integration API confined to tests.
    @OptIn(KoinInternalApi::class)
    private fun resolve(store: ViewModelStore, koin: Koin = application.koin): ShellViewModel =
        resolveViewModel(
            vmClass = ShellViewModel::class,
            viewModelStore = store,
            extras = CreationExtras.Empty,
            scope = koin.scopeRegistry.rootScope
        )
}
