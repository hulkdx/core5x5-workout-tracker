package com.hulkdx.core5x5.app

import androidx.lifecycle.ViewModelStore
import com.hulkdx.core5x5.core.preferences.data.PersistedTrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.core.preferences.domain.Appearance
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var saved: String? = "LB:91:false:true:DARK"
    private var failRead = false
    private var reads = 0
    private val repository = PersistedTrainingPreferencesRepository(object : PreferencesStorage {
        override suspend fun read(): String? {
            reads++
            check(!failRead)
            return saved
        }
        override suspend fun write(value: String) { saved = value }
    })

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun observesSavedAppearanceWithoutResettingItOnResumeAndStopsWithItsOwner() = runTest(dispatcher) {
        val viewModel = AppearanceViewModel(repository).also { store.put("appearance", it) }
        assertNull(viewModel.appearance.value)
        runCurrent()
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        viewModel.refresh()
        repository.setAppearance(Appearance.LIGHT)
        runCurrent()
        assertEquals(Appearance.LIGHT, viewModel.appearance.value)
        repository.setRestSoundEnabled(false)
        runCurrent()
        assertEquals(Appearance.LIGHT, viewModel.appearance.value)
        assertEquals(1, reads)

        store.clear()
        repository.setAppearance(Appearance.DARK)
        runCurrent()
        assertEquals(Appearance.LIGHT, viewModel.appearance.value)
    }

    @Test
    fun failedInitialReadUsesSystemAndResumeRetriesTheSavedChoice() = runTest(dispatcher) {
        failRead = true
        val viewModel = AppearanceViewModel(repository).also { store.put("appearance", it) }
        runCurrent()
        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
        failRead = false
        viewModel.refresh()
        runCurrent()
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        assertEquals(2, reads)
    }
}
