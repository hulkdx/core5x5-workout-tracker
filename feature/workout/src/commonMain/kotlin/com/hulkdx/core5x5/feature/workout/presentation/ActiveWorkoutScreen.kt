package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.hulkdx.core5x5.core.ui.components.Core5x5ExerciseRow
import com.hulkdx.core5x5.core.ui.components.Core5x5PrimaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SetControl
import com.hulkdx.core5x5.core.ui.components.Core5x5SetControls
import com.hulkdx.core5x5.core.ui.components.Core5x5SetState
import com.hulkdx.core5x5.core.ui.components.Core5x5WorkoutTopBar
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
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
    onCompleteSet: (Int, Int) -> Unit,
    onCompleteNextSet: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    val workout = uiState.unfinishedWorkout
    val restVisible = uiState.restTimer.isVisible
    val gap = if (restVisible) Core5x5Dimensions.RestGap else Core5x5Dimensions.ContentGap
    Surface(modifier = modifier.fillMaxSize(), color = Core5x5Colors.Background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            val remainingRows = (workout?.exercises?.size?.minus(1) ?: 0).coerceAtLeast(0)
            // Sum the measured sections, their gaps, and Finish; no device-specific breakpoint.
            val minimumRestHeight = maxOf(Core5x5Dimensions.TopBarHeight, Core5x5Dimensions.TouchTargetMin) +
                Core5x5Dimensions.ExerciseHeaderHeight + Core5x5Dimensions.SetDiameter +
                Core5x5Dimensions.RestVisualHeight + Core5x5Dimensions.TimerHeight +
                Core5x5Dimensions.BorderStroke + Core5x5Dimensions.ExerciseRowHeight * remainingRows.toFloat() +
                Core5x5Dimensions.ButtonHeight + Core5x5Dimensions.ContentPaddingVertical +
                Core5x5Dimensions.RestGap * (7 + remainingRows).toFloat() +
                if (workout?.exercises?.all { item -> item.setStates.all { it.isCompleted } } == true) {
                    with(LocalDensity.current) { Core5x5Typography.Caption.lineHeight.toDp() } + Core5x5Dimensions.RestGap
                } else 0.dp
            val pinTimer = restVisible && (
                maxHeight < minimumRestHeight || maxWidth < Core5x5Dimensions.ReferenceWidth ||
                    LocalDensity.current.fontScale > 1f || uiState.hasLoadError ||
                    uiState.hasSetSaveError || uiState.hasSaveError
                )
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = Core5x5Dimensions.ScreenInset,
                            end = Core5x5Dimensions.ScreenInset,
                            top = gap,
                            bottom = if (pinTimer) 0.dp else Core5x5Dimensions.ContentPaddingVertical,
                        ),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    Core5x5WorkoutTopBar(
                        title = workout?.let { "Workout ${it.workout.name}" } ?: "Workout",
                        onBack = onBack,
                        backEnabled = !uiState.isSaving && !uiState.isCompletingSet,
                    )
                    if (uiState.hasLoadError) {
                        WorkoutError("Unable to load the active workout.")
                        Core5x5PrimaryButton(label = "Try Again", onClick = onRetryLoad)
                    }
                    when {
                        workout != null -> {
                            ActiveWorkoutContent(
                                uiState = uiState,
                                workout = workout,
                                weightUnit = weightUnit,
                                showInlineTimer = restVisible && !pinTimer,
                                onCompleteSet = onCompleteSet,
                                onCompleteNextSet = onCompleteNextSet,
                                onSelectExercise = onSelectExercise,
                            )
                            if (uiState.hasSaveError) {
                                WorkoutError("Unable to confirm the workout was saved. Try finishing again.")
                            }
                            Core5x5SecondaryButton(
                                label = if (uiState.isSaving) "Saving…" else "Finish Workout",
                                onClick = onFinishWorkout,
                                enabled = uiState.canFinish,
                            )
                        }
                        uiState.isLoading -> CircularProgressIndicator(
                            modifier = Modifier.size(Core5x5Dimensions.TouchTargetMin),
                            color = Core5x5Colors.Action,
                            strokeWidth = Core5x5Dimensions.BorderStroke,
                        )
                        !uiState.hasLoadError -> Text(
                            text = "No unfinished workout.",
                            style = Core5x5Typography.Body,
                            color = Core5x5Colors.SecondaryText,
                        )
                    }
                }
                if (pinTimer) {
                    RestTimerScreen(
                        uiState = uiState.restTimer,
                        compact = true,
                        modifier = Modifier.padding(
                            start = Core5x5Dimensions.ScreenInset,
                            end = Core5x5Dimensions.ScreenInset,
                            top = gap,
                            bottom = Core5x5Dimensions.ContentPaddingVertical,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    workout: UnfinishedWorkout,
    weightUnit: WeightUnit,
    showInlineTimer: Boolean,
    onCompleteSet: (Int, Int) -> Unit,
    onCompleteNextSet: () -> Unit,
    onSelectExercise: (Int) -> Unit,
) {
    val exercisePosition = uiState.selectedExercisePosition
    val exercise = workout.exercises.getOrNull(exercisePosition) ?: return
    val nextSet = exercise.setStates.firstOrNull { !it.isCompleted }
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.ExerciseHeaderHeight),
        verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.TextGap, alignment = Alignment.CenterVertically),
    ) {
        Text(
            text = exercise.exercise.displayName(),
            style = Core5x5Typography.Heading,
            color = Core5x5Colors.PrimaryText,
        )
        Text(
            text = exercise.prescription(weightUnit),
            style = Core5x5Typography.Body,
            color = Core5x5Colors.SecondaryText,
        )
    }
    Core5x5SetControls(
        sets = exercise.setStates.map { set ->
            Core5x5SetControl(
                position = set.position,
                state = when {
                    set.isCompleted -> Core5x5SetState.Completed
                    !uiState.canCompleteSet -> Core5x5SetState.Disabled
                    set == nextSet -> Core5x5SetState.Active
                    else -> Core5x5SetState.Default
                },
                completionDescription = "${exercise.exercise.displayName()}, set ${set.position + 1}, ${exercise.reps} reps",
            )
        },
        onComplete = { setPosition -> onCompleteSet(exercisePosition, setPosition) },
    )
    ExerciseVisual(exercise = exercise.exercise, compact = uiState.restTimer.isVisible)
    if (uiState.hasSetSaveError) {
        WorkoutError("Unable to save the set. Tap it again to retry.")
    }
    when {
        showInlineTimer -> RestTimerScreen(uiState.restTimer)
        !uiState.restTimer.isVisible && nextSet != null -> Core5x5PrimaryButton(
            label = if (uiState.isCompletingSet) "Saving…" else "Complete Set ${nextSet.position + 1}",
            onClick = onCompleteNextSet,
            enabled = uiState.canCompleteSet,
        )
    }
    if (workout.exercises.all { item -> item.setStates.all { it.isCompleted } }) {
        Text(
            text = "All sets complete",
            style = Core5x5Typography.Caption,
            color = Core5x5Colors.Action,
        )
    }
    HorizontalDivider(thickness = Core5x5Dimensions.BorderStroke, color = Core5x5Colors.Border)
    workout.exercises.forEachIndexed { position, otherExercise ->
        if (position != exercisePosition) {
            Core5x5ExerciseRow(
                name = otherExercise.exercise.displayName(),
                prescription = otherExercise.prescription(weightUnit),
                isCompleted = otherExercise.setStates.all { it.isCompleted },
                onClick = { onSelectExercise(position) },
                enabled = uiState.canCompleteSet,
            )
        }
    }
}

private fun UnfinishedWorkoutExercise.prescription(weightUnit: WeightUnit): String =
    "$sets × $reps · ${formatWeight(weightKg, weightUnit)}"

@Composable
private fun WorkoutError(message: String) {
    Text(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = Core5x5Typography.Caption,
        color = Core5x5Colors.Destructive,
    )
}

@Preview(name = "Active workout", widthDp = 390, heightDp = 844)
@Composable
private fun ActiveWorkoutPreview() = WorkoutPreview(rest = false)

@Preview(name = "Running rest", widthDp = 390, heightDp = 844)
@Composable
private fun RunningRestWorkoutPreview() = WorkoutPreview(rest = true)

@Preview(name = "Expired rest", widthDp = 390, heightDp = 844)
@Composable
private fun ExpiredRestWorkoutPreview() = WorkoutPreview(rest = true, expired = true)

@Preview(name = "Narrow workout, larger text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun NarrowRestWorkoutPreview() = WorkoutPreview(rest = true, expired = true)

@Composable
private fun WorkoutPreview(rest: Boolean, expired: Boolean = false) {
    val workout = UnfinishedWorkout(
        workout = Workout.A,
        startedAtEpochMillis = 0,
        id = 1,
        restTimer = if (rest) RestTimer(180_000) else null,
        exercises = Workout.A.exercises.mapIndexed { position, exercise ->
            UnfinishedWorkoutExercise(exercise, exercise.sets, exercise.reps, exercise.startingWeightKg).let { prescription ->
                if (rest && position == 0) {
                    prescription.copy(setStates = prescription.setStates.map { it.copy(isCompleted = it.position == 0) })
                } else prescription
            }
        },
    )
    Core5x5Theme {
        ActiveWorkoutScreen(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                unfinishedWorkout = workout,
                restTimer = RestTimerUiState(isVisible = rest, isExpired = expired, countdown = if (expired) "00:00" else "02:30"),
            ),
            onBack = {},
            onFinishWorkout = {},
            onRetryLoad = {},
            onCompleteSet = { _, _ -> },
            onCompleteNextSet = {},
            onSelectExercise = {},
            modifier = Modifier.padding(top = Core5x5Dimensions.ReferenceSystemTopReserve),
        )
    }
}
