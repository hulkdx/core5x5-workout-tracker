package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AppBackStackTest {
    @Test
    fun switchingTabsReplacesThePreviousTabAndBackReturnsToToday() {
        val backStack = mutableStateListOf<AppDestination>(AppDestination.Today)

        backStack.selectNavigationItem(Core5x5NavigationItem.HISTORY)
        backStack.selectNavigationItem(Core5x5NavigationItem.SETTINGS)
        backStack.selectNavigationItem(Core5x5NavigationItem.HISTORY)

        assertEquals(listOf(AppDestination.Today, AppDestination.History), backStack.toList())
        backStack.removeLastOrNull()
        assertEquals(AppDestination.Today, backStack.single())
    }

    @Test
    fun repeatedTabSelectionDoesNotDuplicateDestinations() {
        val backStack = mutableStateListOf<AppDestination>(AppDestination.Today)

        backStack.selectNavigationItem(Core5x5NavigationItem.TODAY)
        backStack.selectNavigationItem(Core5x5NavigationItem.HISTORY)
        backStack.selectNavigationItem(Core5x5NavigationItem.HISTORY)
        assertEquals(listOf(AppDestination.Today, AppDestination.History), backStack.toList())

        backStack.selectNavigationItem(Core5x5NavigationItem.SETTINGS)
        backStack.selectNavigationItem(Core5x5NavigationItem.SETTINGS)
        assertEquals(listOf(AppDestination.Today, AppDestination.Settings), backStack.toList())

        backStack.selectNavigationItem(Core5x5NavigationItem.TODAY)
        backStack.selectNavigationItem(Core5x5NavigationItem.TODAY)
        assertEquals(listOf(AppDestination.Today), backStack.toList())
    }

    @Test
    fun selectedTabRestoresAndBackStillReturnsToToday() {
        listOf(AppDestination.History, AppDestination.Settings).forEach { destination ->
            val original = mutableStateListOf<AppDestination>(AppDestination.Today, destination)
            val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
            val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

            assertNotSame(original, restored)
            assertEquals(listOf(AppDestination.Today, destination), restored.toList())
            assertEquals(destination.navigationItem, restored.last().navigationItem)
            restored.removeLastOrNull()
            assertEquals(AppDestination.Today, restored.single())
        }
    }

    @Test
    fun focusedWorkoutDestinationsHideNavigationAndIgnoreDelayedTabCallbacks() {
        val id = Int.MAX_VALUE.toLong() + 42L
        listOf(
            AppDestination.ActiveWorkout(id),
            AppDestination.WorkoutComplete(id),
            AppDestination.WorkoutDetail(id),
        ).forEach { destination ->
            val backStack = mutableStateListOf<AppDestination>(AppDestination.Today, destination)

            assertNull(destination.navigationItem)
            Core5x5NavigationItem.entries.forEach(backStack::selectNavigationItem)

            assertEquals(listOf(AppDestination.Today, destination), backStack.toList())
        }
    }

    @Test
    fun completionRestoresTheIdentifiedSessionAndReturnsDirectlyToToday() {
        val id = Int.MAX_VALUE.toLong() + 42L
        val original = mutableStateListOf<AppDestination>(
            AppDestination.Today,
            AppDestination.WorkoutComplete(id),
        )
        val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
        val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

        assertNotSame(original, restored)
        assertEquals(original.toList(), restored.toList())
        assertEquals(AppDestination.WorkoutComplete(id), restored.removeLastOrNull())
        assertEquals(listOf(AppDestination.Today), restored.toList())
    }

    @Test
    fun activeDestinationRestoresTheOriginalIdNeededToRecoverACommittedSave() {
        val original = mutableStateListOf<AppDestination>(
            AppDestination.Today,
            AppDestination.ActiveWorkout(42L),
        )
        val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
        val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

        assertEquals(AppDestination.ActiveWorkout(42L), restored.last())
        restored[restored.lastIndex] = AppDestination.WorkoutComplete(42L)
        assertTrue(restored.none { it is AppDestination.ActiveWorkout })
        restored.removeLastOrNull()
        assertEquals(AppDestination.Today, restored.single())
    }

    @Test
    fun workoutDetailRestoresItsStableIdAndReturnsToHistory() {
        val id = Int.MAX_VALUE.toLong() + 42L
        val original = mutableStateListOf<AppDestination>(
            AppDestination.Today,
            AppDestination.History,
            AppDestination.WorkoutDetail(id),
        )
        val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
        val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

        assertEquals(original.toList(), restored.toList())
        assertNull(restored.last().navigationItem)
        assertEquals(AppDestination.WorkoutDetail(id), restored.removeLastOrNull())
        assertEquals(AppDestination.History, restored.last())
    }
}
