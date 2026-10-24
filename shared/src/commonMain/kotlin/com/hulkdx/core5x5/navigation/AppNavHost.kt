package com.hulkdx.core5x5.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.hulkdx.core5x5.core.navigation.CoreNavigator
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
    val push = appDefaultTransitionSpec<NavKey>()
    val pop = appDefaultPopTransitionSpec<NavKey>()
    val predictivePop = appDefaultPredictivePopTransitionSpec<NavKey>()
    NavDisplay(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        entries = navigator.state.toDecoratedEntries(provider),
        onBack = navigator::goBack,
        transitionSpec = {
            if (isTabSwitch(initialState.entries.last().contentKey, targetState.entries.last().contentKey, tabContentKeys)) {
                EnterTransition.None togetherWith ExitTransition.None
            } else push()
        },
        popTransitionSpec = {
            if (isTabSwitch(initialState.entries.last().contentKey, targetState.entries.last().contentKey, tabContentKeys)) {
                EnterTransition.None togetherWith ExitTransition.None
            } else pop()
        },
        predictivePopTransitionSpec = { edge ->
            if (isTabSwitch(initialState.entries.last().contentKey, targetState.entries.last().contentKey, tabContentKeys)) {
                EnterTransition.None togetherWith ExitTransition.None
            } else predictivePop(edge)
        },
    )
}
