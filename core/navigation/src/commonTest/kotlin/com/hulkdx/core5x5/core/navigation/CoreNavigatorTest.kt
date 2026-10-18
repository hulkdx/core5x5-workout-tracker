package com.hulkdx.core5x5.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreNavigatorTest {
    private data object Home : NavKey
    private data object Library : NavKey
    private data object Preferences : NavKey
    private data class Item(val id: Long) : NavKey
    private data class Result(val id: Long) : NavKey
    private val roots = mapOf<TopLevelTab, NavKey>(
        TopLevelTab.TODAY to Home,
        TopLevelTab.HISTORY to Library,
        TopLevelTab.SETTINGS to Preferences,
    )

    @Test
    fun initialStateSelectsHomeWithAnIndependentRootForEveryTab() {
        val state = navigationState()

        assertEquals(Home, state.currentTopLevelKey)
        assertEquals(Home, state.currentKey)
        assertTrue(state.isAtTopLevel)
        assertFalse(state.canGoBack)
        assertEquals(listOf(Home), state.visibleTopLevelKeys)
        assertEquals(roots.values.associateWith { listOf(it) }, state.backStacks)
    }

    @Test
    fun forwardNavigationRetainsTheIdentifiedDestinationAndBackReturnsToTheRoot() {
        val navigator = navigator()
        val item = Item(Long.MAX_VALUE)

        navigator.navigate(item)
        assertEquals(listOf(Home, item), navigator.state.backStacks.getValue(Home))
        assertTrue(navigator.state.canGoBack)

        navigator.goBack()
        assertEquals(Home, navigator.state.currentKey)
        assertFalse(navigator.state.canGoBack)
        navigator.goBack()
        assertEquals(listOf(Home), navigator.state.backStacks.getValue(Home))
    }

    @Test
    fun replacementRemovesTheOldEntryAndBackCannotReopenIt() {
        val navigator = navigator()
        val item = Item(42)
        navigator.navigate(item)
        navigator.replaceIfTop(item, Result(42))

        assertEquals(listOf(Home, Result(42)), navigator.state.backStacks.getValue(Home))
        navigator.goBack()
        assertEquals(Home, navigator.state.currentKey)
    }

    @Test
    fun staleReplacementCannotChangeANewerDestinationOrAnotherTab() {
        val navigator = navigator()
        navigator.navigate(Item(42))
        navigator.navigate(Item(43))
        navigator.replaceIfTop(Item(42), Result(42))
        assertEquals(Item(43), navigator.state.currentKey)

        navigator.goBack()
        navigator.goBack()
        navigator.selectTab(TopLevelTab.HISTORY)
        navigator.replaceIfTop(Item(43), Result(43))
        assertEquals(Library, navigator.state.currentKey)
        navigator.selectTab(TopLevelTab.TODAY)
        assertEquals(Home, navigator.state.currentKey)
    }

    @Test
    fun rootsCannotBePoppedOrReplaced() {
        val navigator = navigator()
        navigator.replaceIfTop(Home, Result(42))
        navigator.goBack()
        assertEquals(Home, navigator.state.currentKey)

        navigator.navigate(Item(42))
        assertFailsWith<IllegalArgumentException> { navigator.replaceIfTop(Item(42), Library) }
        assertEquals(Item(42), navigator.state.currentKey)
    }

    @Test
    fun duplicateNavigationReusesTheExistingEntry() {
        val navigator = navigator()
        navigator.navigate(Item(42))
        navigator.navigate(Item(42))
        assertEquals(listOf(Home, Item(42)), navigator.state.backStacks.getValue(Home))

        navigator.navigate(Item(43))
        navigator.navigate(Item(42))
        assertEquals(listOf(Home, Item(42)), navigator.state.backStacks.getValue(Home))
        navigator.navigate(Item(43))
        navigator.replaceIfTop(Item(43), Item(42))
        assertEquals(listOf(Home, Item(42)), navigator.state.backStacks.getValue(Home))
    }

    @Test
    fun switchingAndReselectingTabsPreservesEachStackWithoutAccumulatingTabVisits() {
        val navigator = navigator()
        navigator.selectTab(TopLevelTab.HISTORY)
        navigator.selectTab(TopLevelTab.SETTINGS)
        navigator.selectTab(TopLevelTab.SETTINGS)
        assertEquals(listOf(Home, Preferences), navigator.state.visibleTopLevelKeys)
        assertEquals(listOf(Preferences), navigator.state.backStacks.getValue(Preferences))

        navigator.selectTab(TopLevelTab.HISTORY)
        navigator.selectTab(TopLevelTab.HISTORY)
        assertEquals(Library, navigator.state.currentKey)
        navigator.goBack()
        assertEquals(Home, navigator.state.currentKey)
        assertEquals(roots.values.toSet(), navigator.state.backStacks.keys)
        assertTrue(navigator.state.backStacks.values.all { it.size == 1 })
    }

    @Test
    fun staleTabCallbacksCannotInterruptAFocusedEntry() {
        val navigator = navigator()
        navigator.navigate(Library)
        navigator.navigate(Item(2))
        TopLevelTab.entries.forEach(navigator::selectTab)
        assertEquals(Item(2), navigator.state.currentKey)
        navigator.goBack()
        navigator.selectTab(TopLevelTab.TODAY)
        assertEquals(Home, navigator.state.currentKey)
    }

    @Test
    fun lateNavigationFromAnInactiveSourceCannotChangeEitherTabStack() {
        val navigator = navigator()
        val delayedRequest = { navigator.navigateFrom(Home, Item(Long.MAX_VALUE)) }
        navigator.selectTab(TopLevelTab.HISTORY)

        delayedRequest()

        assertEquals(Library, navigator.state.currentKey)
        assertEquals(listOf(Home), navigator.state.backStacks.getValue(Home))
        assertEquals(listOf(Library), navigator.state.backStacks.getValue(Library))
        navigator.selectTab(TopLevelTab.TODAY)
        delayedRequest()
        assertEquals(Item(Long.MAX_VALUE), navigator.state.currentKey)
        delayedRequest()
        assertEquals(listOf(Home, Item(Long.MAX_VALUE)), navigator.state.backStacks.getValue(Home))
    }

    @Test
    fun sourceGuardAlsoRejectsANewerEntryInTheSameTab() {
        val navigator = navigator()
        navigator.navigate(Item(1))
        navigator.navigateFrom(Home, Item(2))
        assertEquals(Item(1), navigator.state.currentKey)
        navigator.navigateFrom(Item(1), Result(1))
        assertEquals(Result(1), navigator.state.currentKey)
    }

    @Test
    fun programmaticRootNavigationBypassesTheTabGuardAndClearsOnlyTheTargetStack() {
        val navigator = navigator()
        navigator.navigate(Item(1))
        navigator.selectTab(TopLevelTab.HISTORY)
        assertEquals(Item(1), navigator.state.currentKey)

        navigator.navigate(Library)
        assertEquals(Library, navigator.state.currentKey)
        assertEquals(listOf(Home, Item(1)), navigator.state.backStacks.getValue(Home))
        navigator.navigate(Item(2))
        navigator.navigate(Home)
        assertEquals(Home, navigator.state.currentKey)
        assertEquals(listOf(Home), navigator.state.backStacks.getValue(Home))
        assertEquals(listOf(Library, Item(2)), navigator.state.backStacks.getValue(Library))

        navigator.selectTab(TopLevelTab.HISTORY)
        assertEquals(Item(2), navigator.state.currentKey)
        navigator.navigate(Library)
        assertEquals(listOf(Library), navigator.state.backStacks.getValue(Library))
        navigator.goBack()
        assertEquals(Home, navigator.state.currentKey)
    }

    @Test
    fun programmaticTabNavigationNeverNestsOrDuplicatesTabRoots() {
        roots.values.forEach { source ->
            roots.values.forEach { destination ->
                val navigator = navigator()
                navigator.navigate(source)
                navigator.navigate(Item(42))
                navigator.navigate(destination)
                navigator.navigate(destination)

                assertEquals(destination, navigator.state.currentKey)
                assertEquals(listOf(destination), navigator.state.backStacks.getValue(destination))
                navigator.state.backStacks.forEach { (root, stack) ->
                    assertEquals(root, stack.first())
                    assertTrue(stack.drop(1).none { it in roots.values })
                    assertEquals(stack.size, stack.distinct().size)
                }
            }
        }
    }

    @Test
    fun restoredSelectedTabAndIndependentStacksKeepTheirBackBehavior() {
        val state = NavigationState(
            startKey = Home,
            selectedTab = mutableStateOf<NavKey>(Library),
            stacks = mapOf(
                Home to NavBackStack(Home, Result(1)),
                Library to NavBackStack(Library, Item(Long.MAX_VALUE)),
                Preferences to NavBackStack(Preferences),
            ),
            tabDestinations = roots,
        )
        val navigator = CoreNavigator(state)
        assertEquals(Library, state.currentTopLevelKey)
        assertEquals(Item(Long.MAX_VALUE), state.currentKey)
        assertFalse(state.isAtTopLevel)
        assertTrue(state.canGoBack)
        assertEquals(listOf(Home, Library), state.visibleTopLevelKeys)
        navigator.goBack()
        assertEquals(Library, state.currentKey)
        assertTrue(state.isAtTopLevel)
        assertTrue(state.canGoBack)
        navigator.goBack()
        assertEquals(Result(1), state.currentKey)
    }

    @Test
    fun selectingATabRestoresItsPreviouslySavedNestedStack() {
        val state = NavigationState(
            startKey = Home,
            selectedTab = mutableStateOf<NavKey>(Home),
            stacks = mapOf(
                Home to NavBackStack(Home),
                Library to NavBackStack(Library, Item(Long.MAX_VALUE)),
                Preferences to NavBackStack(Preferences),
            ),
            tabDestinations = roots,
        )
        val navigator = CoreNavigator(state)
        navigator.selectTab(TopLevelTab.SETTINGS)
        navigator.selectTab(TopLevelTab.HISTORY)
        assertEquals(Item(Long.MAX_VALUE), state.currentKey)
        assertEquals(listOf(Home), state.backStacks.getValue(Home))
        assertEquals(listOf(Preferences), state.backStacks.getValue(Preferences))
    }

    private fun navigator() = CoreNavigator(navigationState())

    private fun navigationState() = NavigationState(
        startKey = Home,
        selectedTab = mutableStateOf(Home),
        stacks = roots.values.associateWith { NavBackStack(it) },
        tabDestinations = roots,
    )
}
