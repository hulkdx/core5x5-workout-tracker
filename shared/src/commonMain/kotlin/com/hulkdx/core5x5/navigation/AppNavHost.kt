package com.hulkdx.core5x5.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.core.ui.theme.AppMotion
import com.hulkdx.core5x5.feature.history.navigation.historyEntries
import com.hulkdx.core5x5.feature.settings.navigation.settingsEntries
import com.hulkdx.core5x5.feature.workout.navigation.workoutEntries

@Composable
fun AppNavHost(navigator: CoreNavigator) {
    val provider = entryProvider {
        workoutEntries(navigator)
        historyEntries(navigator)
        settingsEntries(navigator)
    }
    val tabContentKeys = navigator.state.backStacks.mapValues { (_, stack) ->
        stack.map { provider(it).contentKey }
    }
    NavDisplay(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        entries = navigator.state.toDecoratedEntries(provider),
        onBack = navigator::goBack,
        sizeTransform = null,
        transitionSpec = { navigationTransition(tabContentKeys) },
        popTransitionSpec = { navigationTransition(tabContentKeys, isBack = true) },
        // Keep gesture progress linear so a cancelled Back tracks smoothly to its starting point.
        predictivePopTransitionSpec = {
            navigationTransition(tabContentKeys, isBack = true, easing = LinearEasing)
        },
    )
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.navigationTransition(
    tabContentKeys: Map<NavKey, List<Any>>,
    isBack: Boolean = false,
    easing: Easing = FastOutSlowInEasing,
): ContentTransform {
    // Also recognize tab switches that reveal a retained nested destination.
    val initialContentKey = initialState.entries.last().contentKey
    val targetContentKey = targetState.entries.last().contentKey
    val initialTab = tabContentKeys.entries.firstOrNull { initialContentKey in it.value }?.key
    val targetTab = tabContentKeys.entries.firstOrNull { targetContentKey in it.value }?.key
    if (initialTab != null && targetTab != null && initialTab != targetTab) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val direction = if (isBack) -1 else 1
    return slideInHorizontally(tween(AppMotion.NavigationDurationMillis, easing = easing)) { it * direction } togetherWith
        slideOutHorizontally(tween(AppMotion.NavigationDurationMillis, easing = easing)) { -it * direction }
}
