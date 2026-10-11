package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.components.AppCompletionButton
import com.hulkdx.core5x5.core.ui.components.AppCompletionMetrics
import com.hulkdx.core5x5.core.ui.components.AppCompletionSuccessIcon
import com.hulkdx.core5x5.core.ui.components.AppNextWorkoutSummary
import com.hulkdx.core5x5.core.ui.components.AppPrimaryButton
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppTypography
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.core.ui.theme.AppCompleteTokens as Complete

@Composable
internal fun WorkoutCompleteScreen(
    uiState: WorkoutCompleteUiState,
    onRetryLoad: () -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Complete.Background) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(windowInsets)) {
            val viewportHeight = maxHeight
            // The minimum viewport height centers short content; tall content grows and scrolls.
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = Complete.Inset)
                    .padding(bottom = Complete.BottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Complete.ContentGap),
            ) {
                val completed = uiState.completedWorkout
                val next = uiState.nextWorkout
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(
                        min = (viewportHeight - Complete.BottomPadding - Complete.ButtonHeight - Complete.ContentGap)
                            .coerceAtLeast(0.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Complete.ContentGap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when {
                            uiState.isLoading -> Text(
                                text = "Loading saved workout…",
                                style = AppTypography.Body,
                                color = AppColors.SecondaryText,
                                textAlign = TextAlign.Center,
                            )
                            completed != null && next != null -> {
                                AppCompletionSuccessIcon()
                                Spacer(Modifier.height(Complete.SuccessTitleGap))
                                Text(
                                    text = "Workout Complete",
                                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                                    style = Complete.Title,
                                    color = AppColors.PrimaryText,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(Complete.SubtitleGap))
                                Text(
                                    text = "Workout saved. Your next workout is ready.",
                                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                                    style = Complete.Subtitle,
                                    color = AppColors.SecondaryText,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(Complete.MetricsTopGap))
                                AppCompletionMetrics(
                                    duration = completed.durationMillis.formatWorkoutDuration(),
                                    completedSets = "${completed.completedSets} / ${completed.prescribedSets}",
                                )
                                Spacer(Modifier.height(Complete.ContentGap))
                                AppNextWorkoutSummary(
                                    title = "Workout ${next.workout.name}",
                                    indicator = next.workout.name,
                                )
                            }
                            else -> {
                                Text(
                                    text = if (uiState.error == WorkoutCompleteError.NOT_FOUND) {
                                        "No saved completed workout was found."
                                    } else {
                                        "Unable to load the saved workout. Try again."
                                    },
                                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                                    style = AppTypography.Body,
                                    color = AppColors.SecondaryText,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(Complete.ContentGap))
                                AppPrimaryButton(label = "Try Again", onClick = onRetryLoad)
                            }
                        }
                    }
                }
                if (completed != null && next != null && !uiState.isLoading) {
                    AppCompletionButton(label = "Back to Today", onClick = onBackToToday)
                } else {
                    AppSecondaryButton(label = "Back to Today", onClick = onBackToToday)
                }
            }
        }
    }
}

@Preview(name = "Completed A", widthDp = 390, heightDp = 874)
@Composable
private fun WorkoutCompleteScreenPreview() {
    CompletionPreview(Workout.A)
}

@Preview(name = "Partially completed B", widthDp = 390, heightDp = 874)
@Composable
private fun WorkoutCompleteBPreview() {
    CompletionPreview(Workout.B, completedSets = 7)
}

@Preview(name = "Narrow completion", widthDp = 320, heightDp = 640)
@Composable
private fun WorkoutCompleteNarrowPreview() {
    CompletionPreview(Workout.B, completedSets = 7)
}

@Preview(name = "Larger completion text", widthDp = 390, heightDp = 874)
@Composable
private fun WorkoutCompleteLargeTextPreview() {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
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
    AppTheme {
        WorkoutCompleteScreen(
            uiState = WorkoutCompleteUiState(
                isLoading = false,
                completedWorkout = CompletedWorkout(1L, workout, 0L, 2_538_000L, exercises),
                nextWorkout = WorkoutPrescription(workout.nextWorkout()),
            ),
            onRetryLoad = {},
            onBackToToday = {},
            // The source's mock reserve is used only for reference previews, never device insets.
            windowInsets = WindowInsets(top = Complete.SystemTopReserve, bottom = Complete.SystemBottomReserve),
        )
    }
}
