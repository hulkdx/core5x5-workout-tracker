package com.hulkdx.core5x5.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

/** Composition-scoped navigation operations, independent of feature presentation and storage. */
class CoreNavigator(val state: NavigationState) {
    /** A tab key returns that tab to its root; other tabs keep their existing stacks. */
    fun navigate(destination: NavKey) {
        if (destination in state.topLevelKeys) {
            val stack = state.stacks.getValue(destination)
            while (stack.size > 1) stack.removeLastOrNull()
            state.selectedTab.value = destination
            return
        }
        val stack = state.currentStack
        val existingIndex = stack.indexOf(destination)
        if (existingIndex < 0) {
            stack.add(destination)
        } else {
            // Reuse the existing entry and its owner instead of creating duplicate destinations.
            while (stack.lastIndex > existingIndex) stack.removeLastOrNull()
        }
    }

    /** An asynchronous result may navigate only while its originating entry is current. */
    fun navigateFrom(source: NavKey, destination: NavKey): Boolean {
        if (state.currentKey != source) return false
        navigate(destination)
        return true
    }

    fun goBack() {
        val stack = state.currentStack
        when {
            stack.size > 1 -> stack.removeLastOrNull()
            state.currentTopLevelKey != state.startKey -> state.selectedTab.value = state.startKey
        }
    }

    /** Ignore stale asynchronous results and never replace a tab's root. */
    fun replaceIfTop(expected: NavKey, destination: NavKey) {
        val stack = state.currentStack
        if (stack.size <= 1 || stack.last() != expected) return
        require(destination !in state.topLevelKeys) { "A replacement cannot be a tab root" }
        val existingIndex = stack.indexOf(destination)
        if (existingIndex >= 0) {
            while (stack.lastIndex > existingIndex) stack.removeLastOrNull()
        } else {
            stack[stack.lastIndex] = destination
        }
    }

    /** Tab callbacks cannot interrupt a focused flow, including callbacks from an outgoing bar. */
    fun selectTab(tab: TopLevelTab) {
        if (!state.isAtTopLevel) return
        state.selectedTab.value = state.tabDestinations.getValue(tab)
    }
}

@Composable
fun rememberCoreNavigator(state: NavigationState): CoreNavigator =
    remember(state) { CoreNavigator(state) }
