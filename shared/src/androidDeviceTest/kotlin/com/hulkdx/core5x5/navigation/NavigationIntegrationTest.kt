package com.hulkdx.core5x5.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.navigation.rememberCoreNavigator
import com.hulkdx.core5x5.core.navigation.rememberNavigationState
import com.hulkdx.core5x5.feature.history.navigation.HistoryKey
import com.hulkdx.core5x5.feature.history.navigation.WorkoutDetailKey
import com.hulkdx.core5x5.feature.history.navigation.historyEntries
import com.hulkdx.core5x5.feature.settings.navigation.SettingsKey
import com.hulkdx.core5x5.feature.settings.navigation.settingsEntries
import com.hulkdx.core5x5.feature.workout.navigation.ActiveWorkoutKey
import com.hulkdx.core5x5.feature.workout.navigation.TodayKey
import com.hulkdx.core5x5.feature.workout.navigation.WorkoutCompleteKey
import com.hulkdx.core5x5.feature.workout.navigation.workoutEntries
import kotlinx.serialization.PolymorphicSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var navigator: CoreNavigator
    private val models = mutableMapOf<NavKey, EntryViewModel>()
    private val counters = mutableMapOf<NavKey, MutableIntState>()

    @Test
    fun todayOpensTheRequestedWorkoutAndCompletionReplacesItWithTheSameId() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            val id = Long.MAX_VALUE
            navigator.navigateFrom(TodayKey, ActiveWorkoutKey(id))
            assertEquals(ActiveWorkoutKey(id), navigator.state.currentKey)
            navigator.replaceIfTop(ActiveWorkoutKey(id), WorkoutCompleteKey(id))
            assertEquals(listOf(TodayKey, WorkoutCompleteKey(id)), navigator.state.backStacks[TodayKey])
            navigator.goBack()
            assertEquals(TodayKey, navigator.state.currentKey)
        }
    }

    @Test
    fun workoutRequestAfterSwitchingToHistoryCannotEnterHistorysStack() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            val request = { navigator.navigateFrom(TodayKey, ActiveWorkoutKey(Long.MAX_VALUE)) }
            navigator.selectTab(TopLevelTab.HISTORY)
            request()
            assertEquals(HistoryKey, navigator.state.currentKey)
            assertEquals(listOf(HistoryKey), navigator.state.backStacks.getValue(HistoryKey))
            assertEquals(listOf(TodayKey), navigator.state.backStacks.getValue(TodayKey))
        }
    }

    @Test
    fun tabsReturnToTodayAndKeepTheirRootEntries() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            val roots = mapOf(TopLevelTab.TODAY to TodayKey, TopLevelTab.HISTORY to HistoryKey, TopLevelTab.SETTINGS to SettingsKey)
            TopLevelTab.entries.forEach { tab ->
                navigator.selectTab(tab)
                navigator.selectTab(tab)
                assertEquals(roots.getValue(tab), navigator.state.currentTopLevelKey)
                assertEquals(1, navigator.state.backStacks.getValue(navigator.state.currentTopLevelKey).size)
            }
            navigator.goBack()
            assertEquals(TodayKey, navigator.state.currentKey)
        }
    }

    @Test
    fun focusedEntriesIgnoreDelayedTabCallbacksAtTheNavigationBoundary() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            listOf<NavKey>(ActiveWorkoutKey(42), WorkoutCompleteKey(42), WorkoutDetailKey(42)).forEach { key ->
                navigator.navigate(if (key is WorkoutDetailKey) HistoryKey else TodayKey)
                navigator.navigate(key)
                assertFalse(navigator.state.isAtTopLevel)
                TopLevelTab.entries.forEach(navigator::selectTab)
                assertEquals(key, navigator.state.currentKey)
            }
        }
    }

    @Test
    fun detailReturnsToItsHistoryStackThenToToday() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            navigator.selectTab(TopLevelTab.HISTORY)
            navigator.navigate(WorkoutDetailKey(Long.MAX_VALUE))
            navigator.goBack()
            assertEquals(HistoryKey, navigator.state.currentKey)
            navigator.goBack()
            assertEquals(TodayKey, navigator.state.currentKey)
        }
    }

    @Test
    fun allFeaturesRegisterEveryKeyWithStableDistinctContentKeys() {
        compose.setContent { TestHost() }
        compose.runOnIdle {
            val provider = entryProvider {
                workoutEntries(navigator)
                historyEntries(navigator)
                settingsEntries(navigator)
            }
            val keys = listOf(
                TodayKey to "today", HistoryKey to "history", SettingsKey to "settings",
                ActiveWorkoutKey(42) to "active:42", ActiveWorkoutKey(43) to "active:43",
                WorkoutCompleteKey(42) to "complete:42", WorkoutDetailKey(42) to "detail:42",
            )
            keys.forEach { (key, contentKey) ->
                assertEquals(contentKey, provider(key).contentKey)
            }
            assertEquals(keys.size, keys.map { provider(it.first).contentKey }.distinct().size)
        }
    }

    @Test
    fun restoredActiveIdCanRecoverCompletionWithoutReopeningTheActiveDestination() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TestHost() }
        compose.runOnIdle { navigator.navigate(ActiveWorkoutKey(Long.MAX_VALUE)) }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            val active = ActiveWorkoutKey(Long.MAX_VALUE)
            assertEquals(active, navigator.state.currentKey)
            navigator.replaceIfTop(active, WorkoutCompleteKey(Long.MAX_VALUE))
            assertEquals(WorkoutCompleteKey(Long.MAX_VALUE), navigator.state.currentKey)
            navigator.goBack()
            assertEquals(TodayKey, navigator.state.currentKey)
        }
    }

    @Test
    fun tabSwitchingRetainsViewModelsAndSaveableStateAndPoppingClearsOnlyTheRemovedOwner() {
        compose.setContent { TestHost() }
        compose.runOnIdle { navigator.selectTab(TopLevelTab.HISTORY) }
        lateinit var historyModel: EntryViewModel
        compose.runOnIdle {
            historyModel = models.getValue(HistoryKey)
            counters.getValue(HistoryKey).intValue = 7
            navigator.selectTab(TopLevelTab.SETTINGS)
        }
        compose.runOnIdle { navigator.selectTab(TopLevelTab.HISTORY) }
        compose.runOnIdle {
            assertSame(historyModel, models.getValue(HistoryKey))
            assertFalse(historyModel.cleared)
            assertEquals(7, counters.getValue(HistoryKey).intValue)
            navigator.navigate(WorkoutDetailKey(42))
        }
        lateinit var detailModel: EntryViewModel
        compose.runOnIdle {
            detailModel = models.getValue(WorkoutDetailKey(42))
            assertNotSame(historyModel, detailModel)
            navigator.goBack()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(detailModel.cleared)
            assertSame(historyModel, models.getValue(HistoryKey))
            assertFalse(historyModel.cleared)
            navigator.navigate(WorkoutDetailKey(42))
        }
        compose.runOnIdle { assertNotSame(detailModel, models.getValue(WorkoutDetailKey(42))) }
    }

    @Test
    fun completionReplacementClearsActiveAndBackKeepsTheOriginalTodayOwner() {
        compose.setContent { TestHost() }
        lateinit var today: EntryViewModel
        compose.runOnIdle {
            today = models.getValue(TodayKey)
            navigator.navigate(ActiveWorkoutKey(42))
        }
        lateinit var active: EntryViewModel
        compose.runOnIdle {
            active = models.getValue(ActiveWorkoutKey(42))
            navigator.replaceIfTop(ActiveWorkoutKey(42), WorkoutCompleteKey(42))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(active.cleared)
            assertEquals(WorkoutCompleteKey(42), navigator.state.currentKey)
            navigator.goBack()
        }
        compose.runOnIdle {
            assertEquals(TodayKey, navigator.state.currentKey)
            assertSame(today, models.getValue(TodayKey))
            assertFalse(today.cleared)
        }
    }

    @Test
    fun savedStateRestoresSelectedTabParameterizedEntriesAndOffscreenTabState() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TestHost() }
        compose.runOnIdle { counters.getValue(TodayKey).intValue = 3; navigator.selectTab(TopLevelTab.HISTORY) }
        compose.runOnIdle {
            counters.getValue(HistoryKey).intValue = 7
            navigator.navigate(WorkoutDetailKey(Long.MAX_VALUE))
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertEquals(HistoryKey, navigator.state.currentTopLevelKey)
            assertEquals(WorkoutDetailKey(Long.MAX_VALUE), navigator.state.currentKey)
            navigator.goBack()
        }
        compose.runOnIdle {
            assertEquals(7, counters.getValue(HistoryKey).intValue)
            navigator.goBack()
        }
        compose.runOnIdle {
            assertEquals(TodayKey, navigator.state.currentKey)
            assertEquals(3, counters.getValue(TodayKey).intValue)
        }
    }

    @Test
    fun androidSavedStateCodecPreservesEveryFeatureKeyAndLargeLongIds() {
        val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
        val original = NavBackStack<NavKey>(
            TodayKey, HistoryKey, SettingsKey, ActiveWorkoutKey(Long.MAX_VALUE),
            WorkoutCompleteKey(Long.MAX_VALUE), WorkoutDetailKey(Long.MAX_VALUE),
        )
        val saved = encodeToSavedState(serializer, original, navigationSavedStateConfiguration)
        val restored = decodeFromSavedState(serializer, saved, navigationSavedStateConfiguration)
        assertEquals(original.toList(), restored.toList())
    }

    @Test
    fun androidSavedStateCodecRestoresTheSelectedTabAsAMutableValue() {
        val serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
        listOf(TodayKey, HistoryKey, SettingsKey).forEach { key ->
            val original = mutableStateOf<NavKey>(key)
            val saved = encodeToSavedState(serializer, original, navigationSavedStateConfiguration)
            val restored = decodeFromSavedState(serializer, saved, navigationSavedStateConfiguration)
            assertEquals(key, restored.value)
            restored.value = TodayKey
            assertEquals(TodayKey, restored.value)
        }
    }

    @Composable
    private fun TestHost() {
        navigator = rememberCoreNavigator(rememberNavigationState(
            startKey = TodayKey,
            tabDestinations = mapOf(TopLevelTab.TODAY to TodayKey, TopLevelTab.HISTORY to HistoryKey, TopLevelTab.SETTINGS to SettingsKey),
            configuration = navigationSavedStateConfiguration,
        ))
        val provider: (NavKey) -> NavEntry<NavKey> = { key ->
            NavEntry(key, contentKey = key.toString()) {
                val model = viewModel { EntryViewModel() }
                val counter = rememberSaveable { mutableIntStateOf(0) }
                SideEffect { models[key] = model; counters[key] = counter }
                Text("$key: ${counter.intValue}")
            }
        }
        NavDisplay(
            entries = navigator.state.toDecoratedEntries(provider),
            onBack = navigator::goBack,
        )
    }

    private class EntryViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

}
