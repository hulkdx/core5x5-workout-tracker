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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens
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
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditScreen
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditUiState
import com.hulkdx.core5x5.feature.workout.presentation.CustomRestContent
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
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
    @HomeReferenceScreen
    @Composable
    fun Today() = TodaySnapshot(TodayUiState(isLoading = false, nextWorkout = Workout.A))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayB() = TodaySnapshot(TodayUiState(isLoading = false, nextWorkout = Workout.B))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayResume() = TodaySnapshot(TodayUiState(isLoading = false, unfinishedWorkout = unfinishedWorkout(Workout.A)))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkout() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutB() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.B))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRest() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).copy(
        restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
    ))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestB() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.B).copy(
        restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
    ))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestExpired() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).copy(
        restTimer = RestTimerUiState(isVisible = true, isExpired = true),
    ))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestNextExercise() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).let { state ->
        state.copy(
            selectedExercisePosition = 1,
            unfinishedWorkout = state.unfinishedWorkout?.let { session ->
                session.copy(exercises = session.exercises.mapIndexed { index, exercise ->
                    if (index == 0) exercise.copy(setStates = exercise.setStates.map { it.copy(isCompleted = true) })
                    else exercise
                })
            },
            restTimer = RestTimerUiState(isVisible = true, countdown = "03:00"),
        )
    })

    @PreviewTest
    @Preview(name = "Narrow running rest", widthDp = 320, heightDp = 640, fontScale = 2f)
    @Composable
    fun ActiveWorkoutRestNarrow() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).copy(
        restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
    ))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestSaveError() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).copy(
        hasSetSaveError = true,
        restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
    ))

    @PreviewTest
    @Preview(name = "Narrow large text", widthDp = 320, heightDp = 640, fontScale = 2f)
    @Composable
    fun ActiveWorkoutNarrow() = ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).copy(
        restTimer = RestTimerUiState(isVisible = true, isExpired = true, countdown = "00:00"),
    ))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutEdit() = ExerciseEditSnapshot()

    @PreviewTest
    @Preview(name = "Narrow edit, large text", widthDp = 320, heightDp = 640, fontScale = 2f)
    @Composable
    fun ActiveWorkoutEditNarrow() = ExerciseEditSnapshot()

    @PreviewTest
    @Preview(name = "Custom rest sheet", widthDp = 390, heightDp = 410)
    @Composable
    fun ActiveWorkoutEditRest() = Core5x5Theme {
        Box(Modifier.fillMaxSize().background(com.hulkdx.core5x5.core.ui.theme.Core5x5EditTokens.Sheet)) {
            CustomRestContent(150, {}, {}, {})
        }
    }

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
private fun TodaySnapshot(state: TodayUiState) = ScreenFrame(navigationItem = Core5x5NavigationItem.TODAY, showTitle = false) {
    TodayScreen(uiState = state, onStartWorkout = {}, onResumeWorkout = {}, onRetryLoad = {})
}

@Composable
private fun ActiveWorkoutSnapshot(state: ActiveWorkoutUiState) = Core5x5Theme {
    ActiveWorkoutScreen(
        uiState = state,
        onBack = {},
        onFinishWorkout = {},
        onRetryLoad = {},
        onToggleSet = { _, _ -> },
        onEditExercise = {},
        windowInsets = WindowInsets(top = Core5x5HomeTokens.SystemTopReserve, bottom = Core5x5HomeTokens.SystemBottomReserve),
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
    val isToday = navigationItem == Core5x5NavigationItem.TODAY
    val background = if (isToday) Core5x5HomeTokens.Background else Core5x5Colors.Background
    Core5x5Theme {
        Column(modifier = Modifier.fillMaxSize().background(background)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(
                    WindowInsets(top = if (isToday) Core5x5HomeTokens.SystemTopReserve else Core5x5Dimensions.ReferenceSystemTopReserve),
                ),
            ) {
                if (navigationItem != null) {
                    ShellScreen(uiState = ShellUiState(title = title), showTitle = showTitle, backgroundColor = background, content = content)
                } else {
                    content()
                }
            }
            if (navigationItem != null) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isToday) Core5x5HomeTokens.Card else Core5x5Colors.Elevated)
                        .windowInsetsPadding(WindowInsets(bottom = if (isToday) Core5x5HomeTokens.SystemBottomReserve else 0.dp)),
                ) {
                    Core5x5BottomNavigation(selectedItem = navigationItem, onItemSelected = {})
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditSnapshot() = Core5x5Theme {
    ExerciseEditScreen(
        state = ExerciseEditUiState(0, "Squat", "Squat", 80.0, WeightUnit.KG, 5, 1, 5, null, 180_000),
        onDismiss = {}, onSave = {}, onNameChanged = {}, onWeightChanged = {}, onSetsChanged = {},
        onRepsChanged = {}, onRestSelected = {}, onOpenCustomRest = {}, onCustomRestChanged = {}, onApplyCustomRest = {},
        windowInsets = WindowInsets(top = Core5x5HomeTokens.SystemTopReserve, bottom = Core5x5HomeTokens.SystemBottomReserve),
    )
}
