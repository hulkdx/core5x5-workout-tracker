package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.ui.components.Core5x5ExpandedExerciseCard
import com.hulkdx.core5x5.core.ui.components.Core5x5HomeStartButton
import com.hulkdx.core5x5.core.ui.components.Core5x5RestFinishButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SetControl
import com.hulkdx.core5x5.core.ui.components.Core5x5SetControls
import com.hulkdx.core5x5.core.ui.components.Core5x5SetState
import com.hulkdx.core5x5.core.ui.components.Core5x5WorkoutTopBar
import com.hulkdx.core5x5.core.ui.theme.Core5x5ActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.Core5x5RestTokens as Rest
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
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
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val workout = uiState.unfinishedWorkout
    val resting = uiState.restTimer.isVisible
    Surface(modifier = modifier.fillMaxSize(), color = Home.Background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets)) {
            val cardHeight = (if (resting) Rest.CardPaddingVertical else Active.CardPaddingVertical) * 2 +
                (if (resting) Rest.SetGap else Active.SetGap) + Home.TextGap +
                (if (resting) Rest.SetDiameter else Active.SetDiameter).coerceAtLeast(Core5x5Dimensions.TouchTargetMin) +
                with(LocalDensity.current) {
                    if (resting) Rest.Exercise.lineHeight.toDp() + Rest.Metadata.lineHeight.toDp()
                    else Active.Exercise.lineHeight.toDp() + Active.Metadata.lineHeight.toDp()
                }
            val count = workout?.exercises?.size ?: 0
            val inlineHeight = Active.TopGap + Active.TopBarHeight + cardHeight * count.toFloat() +
                Home.BrandGap * (count + 3).toFloat() + Home.ButtonHeight + Rest.TimerHeight
            val pinTimer = resting && (
                maxHeight < inlineHeight || maxWidth < Core5x5Dimensions.ReferenceWidth ||
                    LocalDensity.current.fontScale > 1f || uiState.hasLoadError ||
                    uiState.hasSetSaveError || uiState.hasSaveError
                )
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(start = Active.Inset, end = Active.Inset, top = Active.TopGap,
                            bottom = if (pinTimer) 0.dp else Home.BrandGap),
                ) {
                    Core5x5WorkoutTopBar(
                        title = workout?.let { "Workout ${it.workout.name}" } ?: "Workout",
                        onBack = onBack,
                        backEnabled = !uiState.isSaving && !uiState.isCompletingSet,
                        refined = true,
                    )
                    Spacer(Modifier.height(Active.TopGap))
                    Column(verticalArrangement = Arrangement.spacedBy(Home.BrandGap)) {
                        if (uiState.hasLoadError) {
                            WorkoutError("Unable to load the active workout.")
                            Core5x5HomeStartButton("Try Again", onRetryLoad, enabled = !uiState.isLoading)
                        }
                        when {
                            workout != null -> {
                                workout.exercises.forEachIndexed { exercisePosition, exercise ->
                                    Core5x5ExpandedExerciseCard(
                                        name = exercise.displayName(),
                                        prescription = exercise.prescription(weightUnit),
                                        resting = resting,
                                        onEdit = onEditExercise?.let { edit -> { edit(exercisePosition) } },
                                        editEnabled = uiState.canCompleteSet,
                                    ) {
                                        val nextSet = exercise.setStates.firstOrNull { !it.isCompleted }
                                        Core5x5SetControls(
                                            sets = exercise.setStates.map { set ->
                                                Core5x5SetControl(
                                                    position = set.position,
                                                    state = when {
                                                        set.isCompleted -> Core5x5SetState.Completed
                                                        set == nextSet -> Core5x5SetState.Active
                                                        else -> Core5x5SetState.Default
                                                    },
                                                    completionDescription = "${exercise.displayName()}, set ${set.position + 1}, ${exercise.reps} reps",
                                                )
                                            },
                                            onComplete = { setPosition -> onToggleSet(exercisePosition, setPosition) },
                                            refined = true,
                                            enabled = uiState.canCompleteSet,
                                            resting = resting,
                                            allowUndo = true,
                                        )
                                    }
                                    // Selection is already restored/advanced by the ViewModel.
                                    if (resting && !pinTimer && exercisePosition == uiState.selectedExercisePosition) {
                                        RestTimerScreen(uiState.restTimer)
                                    }
                                }
                                if (uiState.hasEditLoadError) WorkoutError("Unable to open the editor. Tap Edit to retry.")
                                if (uiState.hasSetSaveError) WorkoutError("Unable to save the set. Tap it again to retry.")
                                if (uiState.hasSaveError) WorkoutError("Unable to confirm the workout was saved. Try finishing again.")
                                val finishLabel = if (uiState.isSaving) "Saving…" else "Finish Workout"
                                if (resting) Core5x5RestFinishButton(
                                    label = finishLabel,
                                    onClick = onFinishWorkout,
                                    enabled = uiState.canFinish,
                                ) else Core5x5HomeStartButton(
                                    label = finishLabel,
                                    onClick = onFinishWorkout,
                                    enabled = uiState.canFinish,
                                )
                            }
                            uiState.isLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(Core5x5Dimensions.TouchTargetMin),
                                color = Home.Action,
                                strokeWidth = Home.BorderStroke,
                            )
                            !uiState.hasLoadError -> Text(text = "No unfinished workout.", style = Active.Metadata, color = Home.Secondary)
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
        style = Home.Eyebrow, color = Core5x5Colors.Destructive)
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
                if (position == 0) prescription.copy(setStates = prescription.setStates.map { it.copy(isCompleted = it.position == 0) })
                else prescription
            }
        },
    )
    Core5x5Theme {
        ActiveWorkoutScreen(
            uiState = ActiveWorkoutUiState(isLoading = false, unfinishedWorkout = workout,
                restTimer = RestTimerUiState(isVisible = rest, isExpired = expired, countdown = if (expired) "00:00" else "02:30")),
            onBack = {}, onFinishWorkout = {}, onRetryLoad = {}, onToggleSet = { _, _ -> },
            windowInsets = WindowInsets(top = Home.SystemTopReserve, bottom = Home.SystemBottomReserve),
        )
    }
}
