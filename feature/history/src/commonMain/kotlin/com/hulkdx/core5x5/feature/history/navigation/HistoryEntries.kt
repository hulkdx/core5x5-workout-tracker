package com.hulkdx.core5x5.feature.history.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.feature.history.presentation.HistoryRoute
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailRoute

fun EntryProviderScope<NavKey>.historyEntries(navigator: CoreNavigator) {
    entry<HistoryKey>(clazzContentKey = { "history" }) {
        HistoryRoute(onTabSelected = navigator::selectTab)
    }
    // History cards are intentionally informational; retain Detail for identified/restored entries.
    entry<WorkoutDetailKey>(clazzContentKey = { "detail:${it.workoutId}" }) { key ->
        WorkoutDetailRoute(workoutId = key.workoutId, onBack = navigator::goBack)
    }
}
