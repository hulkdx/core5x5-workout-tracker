package com.hulkdx.core5x5.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation3.runtime.entryProvider
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
    NavDisplay(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        entries = navigator.state.toDecoratedEntries(provider),
        onBack = navigator::goBack,
        transitionSpec = appDefaultTransitionSpec(),
        popTransitionSpec = appDefaultPopTransitionSpec(),
        predictivePopTransitionSpec = appDefaultPredictivePopTransitionSpec(),
    )
}
