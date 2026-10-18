package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.ui.components.AppExpandedExerciseCard
import com.hulkdx.core5x5.core.ui.components.AppHomeStartButton
import com.hulkdx.core5x5.core.ui.components.AppSetControl
import com.hulkdx.core5x5.core.ui.components.AppSetControls
import com.hulkdx.core5x5.core.ui.components.AppSetState
import com.hulkdx.core5x5.core.ui.components.AppWorkoutSwitch
import com.hulkdx.core5x5.core.ui.components.AppWorkoutTopBar
import com.hulkdx.core5x5.core.ui.theme.AppActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppRestTokens as Rest
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.feature.workout.domain.RestTimer
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout

@Composable
internal fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onBack: () -> Unit,
    onFinishWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    onToggleSet: (Int, Int) -> Unit,
    onEditExercise: ((Int) -> Unit)? = null,
    onSwitchWorkout: () -> Unit = {},
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val workout = uiState.unfinishedWorkout
    val resting = uiState.restTimer.isVisible
    Surface(modifier = modifier.fillMaxSize(), color = Home.Background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets)) {
            val cardHeight = Active.CardPaddingVertical * 2 + Active.SetGap + Home.TextGap +
                Active.SetDiameter.coerceAtLeast(AppDimensions.TouchTargetMin) +
                with(LocalDensity.current) {
                    Active.Exercise.lineHeight.toDp() + Active.Metadata.lineHeight.toDp()
                }
            val count = workout?.exercises?.size ?: 0
            val inlineHeight = Active.TopGap + Active.TopBarHeight + AppDimensions.TouchTargetMin + cardHeight * count.toFloat() +
                Home.BrandGap * (count + 3).toFloat() + Home.ButtonHeight + Rest.TimerHeight
            val pinTimer = resting && (
                maxHeight < inlineHeight || maxWidth < AppDimensions.ReferenceWidth ||
                    LocalDensity.current.fontScale > 1f || uiState.hasLoadError ||
                    uiState.hasSetSaveError || uiState.hasSaveError || uiState.hasSwitchError
                )
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = Active.Inset, end = Active.Inset,
                        top = Active.TopGap, bottom = if (pinTimer) 0.dp else Home.BrandGap),
                ) {
                    item(key = "header", contentType = "header") {
                        Column {
                            AppWorkoutTopBar(
                                title = workout?.let { "Workout ${it.workout.name}" } ?: "Workout",
                                onBack = onBack,
                                backEnabled = !uiState.isSaving && !uiState.isCompletingSet && !uiState.isSwitchingWorkout,
                                refined = true,
                            )
                            if (workout != null) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    AppWorkoutSwitch(
                                        label = if (uiState.isSwitchingWorkout) "Switching…"
                                            else "Switch to ${workout.workout.nextWorkout().name}",
                                        onClick = onSwitchWorkout,
                                        enabled = uiState.canSwitchWorkout,
                                    )
                                }
                            }
                            Spacer(Modifier.height(Active.TopGap))
                        }
                    }
                    if (uiState.hasLoadError) {
                        item(key = "load-error", contentType = "load-error") {
                            Column {
                                WorkoutError("Unable to load the active workout.")
                                Spacer(Modifier.height(Home.BrandGap))
                                AppHomeStartButton("Try Again", onRetryLoad, enabled = !uiState.isLoading)
                                Spacer(Modifier.height(Home.BrandGap))
                            }
                        }
                    }
                    when {
                        workout != null -> {
                            if (uiState.hasSwitchError) item(key = "switch-error", contentType = "error") {
                                Column {
                                    WorkoutError("Unable to confirm the workout switch. Tap Switch to retry before logging sets.")
                                    Spacer(Modifier.height(Home.BrandGap))
                                }
                            }
                            workout.exercises.forEachIndexed { exercisePosition, exercise ->
                                item(key = "exercise:$exercisePosition", contentType = "exercise") {
                                    Column {
                                        AppExpandedExerciseCard(
                                            name = exercise.displayName(),
                                            prescription = exercise.prescription(weightUnit),
                                            onEdit = onEditExercise?.let { edit -> { edit(exercisePosition) } },
                                            editEnabled = uiState.canCompleteSet,
                                            dimEditWhenDisabled = !uiState.isCompletingSet,
                                        ) {
                                            val nextSet = exercise.setStates.firstOrNull { !it.isCompleted }
                                            AppSetControls(
                                                sets = exercise.setStates.map { set ->
                                                    val displayedReps = if (set.isCompleted) set.completedReps ?: exercise.reps else exercise.reps
                                                    AppSetControl(
                                                        position = set.position,
                                                        state = when {
                                                            set.isCompleted -> AppSetState.Completed
                                                            set == nextSet -> AppSetState.Active
                                                            else -> AppSetState.Default
                                                        },
                                                        completionDescription = "${exercise.displayName()}, set ${set.position + 1}, " +
                                                            "$displayedReps reps",
                                                        reps = displayedReps,
                                                    )
                                                },
                                                onComplete = { setPosition -> onToggleSet(exercisePosition, setPosition) },
                                                refined = true,
                                                enabled = uiState.canCompleteSet,
                                                allowCompletedTap = true,
                                            )
                                        }
                                        Spacer(Modifier.height(Home.BrandGap))
                                    }
                                }
                                // Stable exercise keys preserve the scroll anchor when rest moves.
                                if (resting && !pinTimer && exercisePosition == uiState.selectedExercisePosition) {
                                    item(key = "rest", contentType = "rest") {
                                        Column {
                                            RestTimerScreen(uiState.restTimer)
                                            Spacer(Modifier.height(Home.BrandGap))
                                        }
                                    }
                                }
                            }
                            if (uiState.hasEditLoadError) item(key = "edit-error", contentType = "error") {
                                Column {
                                    WorkoutError("Unable to open the editor. Tap Edit to retry.")
                                    Spacer(Modifier.height(Home.BrandGap))
                                }
                            }
                            if (uiState.hasSetSaveError) item(key = "set-error", contentType = "error") {
                                Column {
                                    WorkoutError("Unable to save the set. Tap it again to retry.")
                                    Spacer(Modifier.height(Home.BrandGap))
                                }
                            }
                            if (uiState.hasSaveError) item(key = "finish-error", contentType = "error") {
                                Column {
                                    WorkoutError("Unable to confirm the workout was saved. Try finishing again.")
                                    Spacer(Modifier.height(Home.BrandGap))
                                }
                            }
                            item(key = "finish", contentType = "finish") {
                                val finishLabel = if (uiState.isSaving) "Saving…" else "Finish Workout"
                                AppHomeStartButton(
                                    label = finishLabel,
                                    onClick = onFinishWorkout,
                                    enabled = uiState.canFinish,
                                    dimWhenDisabled = false,
                                )
                            }
                        }
                        uiState.isLoading -> item(key = "loading", contentType = "loading") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimensions.TouchTargetMin),
                                color = Home.Action,
                                strokeWidth = Home.BorderStroke,
                            )
                        }
                        !uiState.hasLoadError -> item(key = "empty", contentType = "empty") {
                            Text(text = "No unfinished workout.", style = Active.Metadata, color = Home.Secondary)
                        }
                    }
                }
                if (pinTimer) {
                    RestTimerScreen(
                        uiState = uiState.restTimer,
                        compact = true,
                        modifier = Modifier.padding(start = Active.Inset, end = Active.Inset,
                            top = Home.BrandGap, bottom = Home.BrandGap),
                    )
                }
            }
        }
    }
}

private fun UnfinishedWorkoutExercise.prescription(weightUnit: WeightUnit): String =
    "$sets × $reps · ${formatWeight(weightKg, weightUnit)}"

@Composable
private fun WorkoutError(message: String) {
    Text(text = message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = Home.Eyebrow, color = AppColors.Destructive)
}

@Preview(name = "Active workout", widthDp = 390, heightDp = 874)
@Composable
private fun ActiveWorkoutPreview() = WorkoutPreview(rest = false)

@Preview(name = "Running rest", widthDp = 390, heightDp = 874)
@Composable
private fun RunningRestWorkoutPreview() = WorkoutPreview(rest = true)

@Preview(name = "Expired rest", widthDp = 390, heightDp = 874)
@Composable
private fun ExpiredRestWorkoutPreview() = WorkoutPreview(rest = true, expired = true)

@Preview(name = "Narrow workout, larger text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun NarrowRestWorkoutPreview() = WorkoutPreview(rest = true, expired = true)

@Composable
private fun WorkoutPreview(rest: Boolean, expired: Boolean = false) {
    val workout = UnfinishedWorkout(
        workout = Workout.A, startedAtEpochMillis = 0, id = 1,
        restTimer = if (rest) RestTimer(180_000) else null,
        exercises = Workout.A.exercises.mapIndexed { position, exercise ->
            UnfinishedWorkoutExercise(exercise, exercise.sets, exercise.reps, exercise.startingWeightKg).let { prescription ->
                if (position == 0) prescription.copy(setStates = prescription.setStates.map { it.copy(isCompleted = it.position == 0, completedReps = if (it.position == 0) prescription.reps else null) })
                else prescription
            }
        },
    )
    AppTheme {
        ActiveWorkoutScreen(
            uiState = ActiveWorkoutUiState(isLoading = false, unfinishedWorkout = workout,
                restTimer = RestTimerUiState(isVisible = rest, isExpired = expired, countdown = if (expired) "00:00" else "02:30")),
            onBack = {}, onFinishWorkout = {}, onRetryLoad = {}, onToggleSet = { _, _ -> },
            windowInsets = WindowInsets(top = Home.SystemTopReserve, bottom = Home.SystemBottomReserve),
        )
    }
}
