package com.hulkdx.core5x5.screenshottest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppCompleteTokens
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.feature.history.presentation.HistoryError
import com.hulkdx.core5x5.feature.history.presentation.HistoryScreen
import com.hulkdx.core5x5.feature.history.presentation.HistoryUiState
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailError
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailScreen
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailUiState
import com.hulkdx.core5x5.feature.settings.presentation.SettingsDialog
import com.hulkdx.core5x5.feature.settings.presentation.SettingsError
import com.hulkdx.core5x5.feature.settings.presentation.SettingsScreen
import com.hulkdx.core5x5.feature.settings.presentation.SettingsUiState
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutScreen
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutUiState
import com.hulkdx.core5x5.feature.workout.presentation.CustomRestContent
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditScreen
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditUiState
import com.hulkdx.core5x5.feature.workout.presentation.RestTimerScreen
import com.hulkdx.core5x5.feature.workout.presentation.RestTimerUiState
import com.hulkdx.core5x5.feature.workout.presentation.TodayError
import com.hulkdx.core5x5.feature.workout.presentation.TodayScreen
import com.hulkdx.core5x5.feature.workout.presentation.TodayUiState
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteError
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteScreen
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteUiState

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
    fun TodayResume() = TodaySnapshot(
        TodayUiState(
            isLoading = false,
            unfinishedWorkout = unfinishedWorkout(Workout.A)
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayLoading() = TodaySnapshot(TodayUiState())

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayLoadError() = TodaySnapshot(TodayUiState(isLoading = false, error = TodayError.LOAD))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayStartError() = TodaySnapshot(
        TodayUiState(isLoading = false, nextWorkout = Workout.A, error = TodayError.START),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodaySwitchError() = TodaySnapshot(
        TodayUiState(isLoading = false, nextWorkout = Workout.B, error = TodayError.SWITCH),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun TodayWorking() = TodaySnapshot(
        TodayUiState(isLoading = false, nextWorkout = Workout.A, isWorking = true),
    )

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
    fun ActiveWorkoutBeforeFirstTap() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(recordedReps = listOf(null, null, null, null, null)),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutSwitchB() =
        ActiveWorkoutSnapshot(activeWorkoutReference(Workout.B).let { state ->
            state.copy(unfinishedWorkout = state.unfinishedWorkout?.let { workout ->
                workout.copy(exercises = workout.exercises.map { exercise ->
                    exercise.copy(setStates = exercise.setStates.map {
                        it.copy(
                            isCompleted = false,
                            completedReps = null
                        )
                    })
                })
            })
        })

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutSwitchSaving() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(recordedReps = listOf(null, null, null, null, null)).copy(
            isSwitchingWorkout = true
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutSwitchError() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(recordedReps = listOf(null, null, null, null, null)).copy(
            hasSwitchError = true
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutAfterFirstTap() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            restTimer = RestTimerUiState(isVisible = true, countdown = "03:00"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutSetSaving() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            isCompletingSet = true,
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestSetSaving() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            isCompletingSet = true,
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutFinishing() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            isSaving = true,
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestFinishing() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            isSaving = true,
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRepCounts() = ActiveWorkoutSnapshot(activeWorkoutRepReference())

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutCustomReps() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(targetReps = 8, recordedReps = listOf(8, 7, 1, null, null)),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutScrollableSets() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(recordedReps = List(12) { if (it < 7) 5 else null }),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutReset() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference(recordedReps = listOf(null, 4, 3, 2, 1)),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRepCountsRest() = ActiveWorkoutSnapshot(
        activeWorkoutRepReference().copy(
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRest() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestB() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.B).copy(
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestExpired() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            restTimer = RestTimerUiState(isVisible = true, isExpired = true),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestExpiredB() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.B).copy(
            restTimer = RestTimerUiState(isVisible = true, isExpired = true),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestNextExercise() =
        ActiveWorkoutSnapshot(activeWorkoutReference(Workout.A).let { state ->
            state.copy(
                selectedExercisePosition = 1,
                unfinishedWorkout = state.unfinishedWorkout?.let { session ->
                    session.copy(exercises = session.exercises.mapIndexed { index, exercise ->
                        if (index == 0) exercise.copy(setStates = exercise.setStates.map {
                            it.copy(
                                isCompleted = true,
                                completedReps = exercise.reps
                            )
                        })
                        else exercise
                    })
                },
                restTimer = RestTimerUiState(isVisible = true, countdown = "03:00"),
            )
        })

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutRestSaveError() = ActiveWorkoutSnapshot(
        activeWorkoutReference(Workout.A).copy(
            hasSetSaveError = true,
            restTimer = RestTimerUiState(isVisible = true, countdown = "02:30"),
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutEdit() = ExerciseEditSnapshot()

    @PreviewTest
    @Preview(name = "Custom rest sheet", widthDp = 432, heightDp = 768)
    @Composable
    fun ActiveWorkoutEditRest() = AppTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(com.hulkdx.core5x5.core.ui.theme.AppEditTokens.Sheet)
        ) {
            CustomRestContent(150, {}, {}, {})
        }
    }

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun ActiveWorkoutEditRestZero() = AppTheme {
        Box(Modifier.fillMaxSize().background(com.hulkdx.core5x5.core.ui.theme.AppEditTokens.Sheet)) {
            CustomRestContent(0, {}, {}, {})
        }
    }

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun RestTimerScaffold() = ScreenFrame {
        RestTimerScreen(uiState = RestTimerUiState(), modifier = Modifier.fillMaxSize())
    }

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun WorkoutComplete() = WorkoutCompleteSnapshot(completedWorkoutState(Workout.A))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun WorkoutCompleteB() =
        WorkoutCompleteSnapshot(completedWorkoutState(Workout.B, completedSets = 7))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun WorkoutCompleteLoadError() = WorkoutCompleteSnapshot(
        WorkoutCompleteUiState(isLoading = false, error = WorkoutCompleteError.LOAD),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun WorkoutCompleteLoading() = WorkoutCompleteSnapshot(WorkoutCompleteUiState())

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun WorkoutCompleteMissing() = WorkoutCompleteSnapshot(
        WorkoutCompleteUiState(isLoading = false, error = WorkoutCompleteError.NOT_FOUND),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun History() = HistorySnapshot(
        HistoryUiState(
            isLoading = false,
            completedWorkouts = historyGraphRecords()
        )
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryGraphSingle() = HistorySnapshot(
        HistoryUiState(isLoading = false, completedWorkouts = historyGraphRecords().take(1)),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryGraphFlat() = HistorySnapshot(
        HistoryUiState(isLoading = false, completedWorkouts = historyReferenceRecords()),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryGraphZero() = HistorySnapshot(
        HistoryUiState(isLoading = false, completedWorkouts = historyGraphRecords().map { workout ->
            workout.copy(exercises = workout.exercises.map { it.copy(weightKg = 0.0) })
        }),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryGraphNoLift() = HistorySnapshot(
        HistoryUiState(
            isLoading = false, completedWorkouts = historyGraphRecords().take(1),
            selectedExercise = CompletedExerciseType.DEADLIFT
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryGraphRefreshError() = HistorySnapshot(
        HistoryUiState(
            isLoading = false,
            completedWorkouts = historyGraphRecords(),
            error = HistoryError.LOAD
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryEmpty() = HistorySnapshot(HistoryUiState(isLoading = false))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun HistoryLoadError() =
        HistorySnapshot(HistoryUiState(isLoading = false, error = HistoryError.LOAD))

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
    fun WorkoutDetailLoading() = WorkoutDetailSnapshot(WorkoutDetailUiState(workoutId = 4L))

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun WorkoutDetailLoadError() = WorkoutDetailSnapshot(
        WorkoutDetailUiState(workoutId = 4L, isLoading = false, error = WorkoutDetailError.LOAD),
    )

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun WorkoutDetailMissing() = WorkoutDetailSnapshot(
        WorkoutDetailUiState(
            workoutId = 4L,
            isLoading = false,
            error = WorkoutDetailError.NOT_FOUND
        ),
    )

    @PreviewTest
    @ReferenceScreen
    @Composable
    fun WorkoutDetailNumericReps() = WorkoutDetailSnapshot(
        WorkoutDetailUiState(
            workoutId = 4L,
            isLoading = false,
            completedWorkout = detailRepRecord()
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun Settings() = SettingsSnapshot(
        SettingsUiState(
            isLoading = false,
            preferences = TrainingPreferences(),
            versionName = "1.0.0"
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsLoading() = SettingsSnapshot(SettingsUiState(versionName = "1.0.0"))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsLoadError() = SettingsSnapshot(
        SettingsUiState(isLoading = false, error = SettingsError.LOAD, versionName = "1.0.0"),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsPrivacyError() = SettingsSnapshot(
        SettingsUiState(
            isLoading = false,
            preferences = TrainingPreferences(),
            versionName = "1.0.0",
            error = SettingsError.OPEN_PRIVACY_POLICY,
        ),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsUnits() = SettingsSnapshot(settingsDialogState(SettingsDialog.UNITS))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsUnitsSaveError() = SettingsSnapshot(
        settingsDialogState(SettingsDialog.UNITS).copy(error = SettingsError.SAVE),
    )

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsAbout() = SettingsSnapshot(settingsDialogState(SettingsDialog.ABOUT))

    @PreviewTest
    @HomeReferenceScreen
    @Composable
    fun SettingsLicense() = SettingsSnapshot(settingsDialogState(SettingsDialog.LICENSE))
}

private fun settingsDialogState(dialog: SettingsDialog) = SettingsUiState(
    isLoading = false,
    preferences = TrainingPreferences(WeightUnit.LB),
    versionName = "1.0.0",
    dialog = dialog,
)

@Composable
private fun WorkoutDetailSnapshot(state: WorkoutDetailUiState) = ScreenFrame {
    WorkoutDetailScreen(
        uiState = state,
        onBack = {},
        onRetryLoad = {},
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun HistorySnapshot(state: HistoryUiState) = ScreenFrame(
    navigationItem = TopLevelTab.HISTORY,
) {
    HistoryScreen(uiState = state, onRetryLoad = {}, weightUnit = WeightUnit.KG)
}

@Composable
private fun TodaySnapshot(state: TodayUiState) = ScreenFrame(navigationItem = TopLevelTab.TODAY) {
    TodayScreen(uiState = state, onStartWorkout = {}, onResumeWorkout = {}, onRetryLoad = {})
}

@Composable
private fun ActiveWorkoutSnapshot(state: ActiveWorkoutUiState) = AppTheme {
    ActiveWorkoutScreen(
        uiState = state,
        onBack = {},
        onFinishWorkout = {},
        onRetryLoad = {},
        onToggleSet = { _, _ -> },
        onEditExercise = {},
        windowInsets = WindowInsets(
            top = AppHomeTokens.SystemTopReserve,
            bottom = AppHomeTokens.SystemBottomReserve
        ),
    )
}

@Composable
private fun WorkoutCompleteSnapshot(state: WorkoutCompleteUiState) = AppTheme {
    WorkoutCompleteScreen(
        uiState = state,
        onRetryLoad = {},
        onBackToToday = {},
        windowInsets = WindowInsets(
            top = AppCompleteTokens.SystemTopReserve,
            bottom = AppCompleteTokens.SystemBottomReserve,
        ),
    )
}

@Composable
private fun SettingsSnapshot(state: SettingsUiState) = ScreenFrame(
    navigationItem = TopLevelTab.SETTINGS,
) {
    SettingsScreen(
        uiState = state,
        onOpenUnits = {},
        onOpenAbout = {},
        onOpenLicense = {},
        onSelectUnit = {},
        onDismissDialog = {},
        onRetryLoad = {},
        onOpenRepository = {},
        onOpenPrivacyPolicy = {},
        onOpenPreciseTimingSettings = {},
    )
}

@Composable
private fun ScreenFrame(
    navigationItem: TopLevelTab? = null,
    content: @Composable () -> Unit,
) {
    AppTheme {
        if (navigationItem != null) {
            TopLevelScreenLayout(
                selectedTab = navigationItem,
                onTabSelected = {},
                windowInsets = WindowInsets(
                    top = AppHomeTokens.SystemTopReserve,
                    bottom = AppHomeTokens.SystemBottomReserve
                ),
                content = content,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .windowInsetsPadding(WindowInsets(top = AppDimensions.ReferenceSystemTopReserve)),
            ) { content() }
        }
    }
}

@Composable
private fun ExerciseEditSnapshot() = AppTheme {
    ExerciseEditScreen(
        state = ExerciseEditUiState(0, "Squat", 80.0, WeightUnit.KG, 5, 5, null, 180_000),
        onDismiss = {},
        onSave = {},
        onWeightChanged = {},
        onSetsChanged = {},
        onRepsChanged = {},
        onRestSelected = {},
        onOpenCustomRest = {},
        onCustomRestChanged = {},
        onApplyCustomRest = {},
        windowInsets = WindowInsets(
            top = AppHomeTokens.SystemTopReserve,
            bottom = AppHomeTokens.SystemBottomReserve
        ),
    )
}
