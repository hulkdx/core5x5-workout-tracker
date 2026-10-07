package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.ui.components.Core5x5ExerciseRow
import com.hulkdx.core5x5.core.ui.components.Core5x5Metrics
import com.hulkdx.core5x5.core.ui.components.Core5x5PrimaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5WorkoutCard
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

@Composable
internal fun WorkoutCompleteScreen(
    uiState: WorkoutCompleteUiState,
    onRetryLoad: () -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Core5x5Colors.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Core5x5Dimensions.ScreenInset,
                    end = Core5x5Dimensions.ScreenInset,
                    top = Core5x5Dimensions.CompleteExtraTop + Core5x5Dimensions.CompleteGap,
                    bottom = Core5x5Dimensions.CompletePaddingBottom,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.CompleteGap),
        ) {
            val completed = uiState.completedWorkout
            val next = uiState.nextWorkout
            when {
                uiState.isLoading -> Text(
                    text = "Loading saved workout…",
                    style = Core5x5Typography.Body,
                    color = Core5x5Colors.SecondaryText,
                )
                completed != null && next != null -> {
                    SuccessIcon()
                    Text(
                        text = "Workout ${completed.workout.name} complete",
                        modifier = Modifier.fillMaxWidth().semantics { heading() },
                        style = Core5x5Typography.Title,
                        color = Core5x5Colors.PrimaryText,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Workout saved. Your next workout is ready.",
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        style = Core5x5Typography.Body,
                        color = Core5x5Colors.SecondaryText,
                        textAlign = TextAlign.Center,
                    )
                    Core5x5Metrics(
                        firstLabel = "Duration",
                        firstValue = completed.durationMillis.formatWorkoutDuration(),
                        secondLabel = "Completed sets",
                        secondValue = "${completed.completedSets} / ${completed.prescribedSets}",
                    )
                    Core5x5WorkoutCard(
                        eyebrow = "NEXT UP",
                        title = "Workout ${next.workout.name}",
                        indicator = next.workout.name,
                    ) {
                        next.exercises.forEach { exercise ->
                            Core5x5ExerciseRow(
                                name = exercise.displayName(),
                                prescription = "${exercise.sets} × ${exercise.reps} · ${formatWeight(exercise.weightKg, weightUnit)}",
                            )
                        }
                    }
                    Core5x5PrimaryButton(label = "Back to Today", onClick = onBackToToday)
                }
                else -> {
                    Text(
                        text = if (uiState.error == WorkoutCompleteError.NOT_FOUND) {
                            "No saved completed workout was found."
                        } else {
                            "Unable to load the saved workout. Try again."
                        },
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        style = Core5x5Typography.Body,
                        color = Core5x5Colors.SecondaryText,
                        textAlign = TextAlign.Center,
                    )
                    Core5x5PrimaryButton(label = "Try Again", onClick = onRetryLoad)
                    Core5x5SecondaryButton(label = "Back to Today", onClick = onBackToToday)
                }
            }
        }
    }
}

@Composable
private fun SuccessIcon() {
    val size = maxOf(
        Core5x5Dimensions.SuccessSize,
        with(LocalDensity.current) { Core5x5Typography.Title.lineHeight.toDp() },
    )
    Surface(shape = CircleShape, color = Core5x5Colors.Action) {
        Box(
            modifier = Modifier.size(size).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", style = Core5x5Typography.Title, color = Core5x5Colors.Background)
        }
    }
}

@Preview(name = "Completed A", widthDp = 390, heightDp = 844)
@Composable
private fun WorkoutCompleteScreenPreview() {
    CompletionPreview(Workout.A)
}

@Preview(name = "Partially completed B", widthDp = 390, heightDp = 844)
@Composable
private fun WorkoutCompleteBPreview() {
    CompletionPreview(Workout.B, completedSets = 7)
}

@Preview(name = "Narrow completion", widthDp = 320, heightDp = 640)
@Composable
private fun WorkoutCompleteNarrowPreview() {
    CompletionPreview(Workout.B, completedSets = 7)
}

@Preview(name = "Larger completion text", widthDp = 390, heightDp = 844)
@Composable
private fun WorkoutCompleteLargeTextPreview() {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.5f)) {
        CompletionPreview(Workout.A)
    }
}

@Composable
private fun CompletionPreview(workout: Workout, completedSets: Int = workout.exercises.sumOf { it.sets }) {
    var remainingCompleted = completedSets
    val exercises = workout.exercises.map { exercise ->
        UnfinishedWorkoutExercise(
            exercise, exercise.sets, exercise.reps, exercise.startingWeightKg,
            setStates = List(exercise.sets) { position ->
                UnfinishedWorkoutSet(position, isCompleted = remainingCompleted-- > 0)
            },
        )
    }
    Core5x5Theme {
        WorkoutCompleteScreen(
            uiState = WorkoutCompleteUiState(
                isLoading = false,
                completedWorkout = CompletedWorkout(1L, workout, 0L, 2_538_000L, exercises),
                nextWorkout = WorkoutPrescription(workout.nextWorkout()),
            ),
            onRetryLoad = {},
            onBackToToday = {},
            // The source's mock reserve is used only for reference previews, never device insets.
            windowInsets = WindowInsets(top = Core5x5Dimensions.ReferenceSystemTopReserve),
        )
    }
}
