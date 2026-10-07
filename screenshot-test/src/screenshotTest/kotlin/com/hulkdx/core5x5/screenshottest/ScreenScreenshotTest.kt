package com.hulkdx.core5x5.screenshottest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.tools.screenshot.PreviewTest
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.feature.history.presentation.HistoryScreen
import com.hulkdx.core5x5.feature.history.presentation.HistoryUiState
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailScreen
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailUiState
import com.hulkdx.core5x5.feature.settings.presentation.SettingsScreen
import com.hulkdx.core5x5.feature.settings.presentation.SettingsUiState
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutScreen
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutUiState
import com.hulkdx.core5x5.feature.workout.presentation.RestTimerScreen
import com.hulkdx.core5x5.feature.workout.presentation.RestTimerUiState
import com.hulkdx.core5x5.feature.workout.presentation.TodayScreen
import com.hulkdx.core5x5.feature.workout.presentation.TodayUiState
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteScreen
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteUiState
import com.hulkdx.core5x5.shell.ShellScreen
import com.hulkdx.core5x5.shell.ShellUiState

class ScreenScreenshotTest {
    @PreviewTest
    @ReferenceScreen
    @Composable
    fun Today() = TodaySnapshot(TodayUiState(isLoading = false, nextWorkout = Workout.A))

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun ActiveWorkout() = ActiveWorkoutSnapshot(
        ActiveWorkoutUiState(isLoading = false, unfinishedWorkout = unfinishedWorkout(Workout.A)),
    )

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun RestTimerScaffold() = ScreenFrame {
        RestTimerScreen(uiState = RestTimerUiState(), modifier = Modifier.fillMaxSize())
    }

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun WorkoutComplete() = WorkoutCompleteSnapshot(completedWorkoutState(Workout.A))

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun HistoryScaffold() = ScreenFrame(
        navigationItem = Core5x5NavigationItem.HISTORY,
        showTitle = false,
    ) {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false, completedWorkouts = historyRecords()),
            onWorkoutSelected = {},
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun WorkoutDetailScaffold() = ScreenFrame {
        WorkoutDetailScreen(
            uiState = WorkoutDetailUiState(
                workoutId = 1L,
                isLoading = false,
                completedWorkout = historyRecords().first(),
            ),
            onBack = {},
            onRetryLoad = {},
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize(),
        )
    }

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun Settings() = SettingsSnapshot(
        SettingsUiState(isLoading = false, preferences = TrainingPreferences(), versionName = "1.0"),
    )
}

@Composable
private fun TodaySnapshot(state: TodayUiState) = ScreenFrame(navigationItem = Core5x5NavigationItem.TODAY) {
    TodayScreen(uiState = state, onStartWorkout = {}, onResumeWorkout = {}, onRetryLoad = {})
}

@Composable
private fun ActiveWorkoutSnapshot(state: ActiveWorkoutUiState) = ScreenFrame {
    ActiveWorkoutScreen(
        uiState = state,
        onBack = {},
        onFinishWorkout = {},
        onRetryLoad = {},
        onCompleteSet = { _, _ -> },
        onCompleteNextSet = {},
        onSelectExercise = {},
    )
}

@Composable
private fun WorkoutCompleteSnapshot(state: WorkoutCompleteUiState) = ScreenFrame {
    WorkoutCompleteScreen(
        uiState = state,
        onRetryLoad = {},
        onBackToToday = {},
        // ScreenFrame already owns the preview's simulated top inset.
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@Composable
private fun SettingsSnapshot(state: SettingsUiState) = ScreenFrame(
    navigationItem = Core5x5NavigationItem.SETTINGS,
    showTitle = false,
) {
    SettingsScreen(
        uiState = state,
        onOpenUnits = {},
        onOpenRestDuration = {},
        onOpenAbout = {},
        onOpenLicense = {},
        onSelectUnit = {},
        onRestDurationInput = {},
        onSaveRestDuration = {},
        onDismissDialog = {},
        onRetryLoad = {},
        onOpenRepository = {},
    )
}

@Composable
private fun ScreenFrame(
    navigationItem: Core5x5NavigationItem? = null,
    title: String = "Core5x5",
    showTitle: Boolean = true,
    content: @Composable () -> Unit,
) {
    Core5x5Theme {
        Column(modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(
                    WindowInsets(top = Core5x5Dimensions.ReferenceSystemTopReserve),
                ),
            ) {
                if (navigationItem != null) {
                    ShellScreen(uiState = ShellUiState(title = title), showTitle = showTitle, content = content)
                } else {
                    content()
                }
            }
            if (navigationItem != null) {
                Core5x5BottomNavigation(
                    selectedItem = navigationItem,
                    onItemSelected = {},
                )
            }
        }
    }
}
