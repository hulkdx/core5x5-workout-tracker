package com.hulkdx.core5x5.feature.workout.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutRoute
import com.hulkdx.core5x5.feature.workout.presentation.TodayRoute
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteRoute

fun EntryProviderScope<NavKey>.workoutEntries(navigator: CoreNavigator) {
    entry<TodayKey>(clazzContentKey = { "today" }) {
        TodayRoute(
            onWorkoutRequested = { navigator.navigateFrom(TodayKey, ActiveWorkoutKey(it)) },
            onTabSelected = navigator::selectTab,
        )
    }
    entry<ActiveWorkoutKey>(clazzContentKey = { "active:${it.workoutId}" }) { key ->
        ActiveWorkoutRoute(
            workoutId = key.workoutId,
            onBack = navigator::goBack,
            onWorkoutCompleted = { completedId ->
                if (completedId == key.workoutId) {
                    navigator.replaceIfTop(key, WorkoutCompleteKey(completedId))
                }
            },
        )
    }
    entry<WorkoutCompleteKey>(clazzContentKey = { "complete:${it.workoutId}" }) { key ->
        WorkoutCompleteRoute(workoutId = key.workoutId, onBackToToday = navigator::goBack)
    }
}
