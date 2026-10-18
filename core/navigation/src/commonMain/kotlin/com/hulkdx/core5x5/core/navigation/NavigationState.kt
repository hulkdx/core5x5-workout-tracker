package com.hulkdx.core5x5.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer

/** Observable navigation state. Use [CoreNavigator] to change the retained tab stacks. */
class NavigationState constructor(
    val startKey: NavKey,
    val selectedTab: MutableState<NavKey>,
    val stacks: Map<NavKey, NavBackStack<NavKey>>,
    val tabDestinations: Map<TopLevelTab, NavKey>,
) {
    val topLevelKeys: Set<NavKey> get() = stacks.keys
    val currentTopLevelKey: NavKey get() = selectedTab.value
    val currentStack: NavBackStack<NavKey> get() = stacks.getValue(currentTopLevelKey)
    val currentKey: NavKey get() = currentStack.last()
    val isAtTopLevel: Boolean get() = currentStack.size == 1
    val canGoBack: Boolean get() = !isAtTopLevel || currentTopLevelKey != startKey
    val backStacks: Map<NavKey, List<NavKey>> get() = stacks

    /** Exit through the start tab without accumulating a history of tab selections. */
    val visibleTopLevelKeys: List<NavKey>
        get() {
            return if (currentTopLevelKey == startKey) listOf(startKey)
            else listOf(startKey, currentTopLevelKey)
        }
}

/** Uses Navigation 3's KMP serializer for the selected tab and every independent stack. */
@Composable
fun rememberNavigationState(
    startKey: NavKey,
    tabDestinations: Map<TopLevelTab, NavKey>,
    configuration: SavedStateConfiguration,
): NavigationState {
    val selectedTab = rememberSerializable(
        startKey, tabDestinations,
        configuration = configuration,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class)),
    ) { mutableStateOf(startKey) }
    val stacks = tabDestinations.values.associateWith { root ->
        key(root) { rememberNavBackStack(configuration, root) }
    }
    return remember(startKey, selectedTab, stacks, tabDestinations) {
        NavigationState(startKey, selectedTab, stacks, tabDestinations.toMap())
    }
}
