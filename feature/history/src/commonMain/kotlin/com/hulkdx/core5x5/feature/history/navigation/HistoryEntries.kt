package com.hulkdx.core5x5.feature.history.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.feature.history.presentation.HistoryRoute
import com.hulkdx.core5x5.feature.history.presentation.HistoryGraphRoute
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailRoute

fun EntryProviderScope<NavKey>.historyEntries(navigator: CoreNavigator) {
    entry<HistoryKey>(clazzContentKey = { "history" }) {
        HistoryRoute(onTabSelected = navigator::selectTab, onOpenGraph = { navigator.navigate(HistoryGraphKey) })
    }
    entry<HistoryGraphKey>(clazzContentKey = { "history-graph" }) {
        HistoryGraphRoute(onBack = navigator::goBack)
    }
    // History cards are intentionally informational; retain Detail for identified/restored entries.
    entry<WorkoutDetailKey>(clazzContentKey = { "detail:${it.workoutId}" }) { key ->
        WorkoutDetailRoute(workoutId = key.workoutId, onBack = navigator::goBack)
    }
}
