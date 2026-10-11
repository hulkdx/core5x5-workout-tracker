package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import com.hulkdx.core5x5.core.ui.components.AppExerciseRow
import com.hulkdx.core5x5.core.ui.components.AppMetrics
import com.hulkdx.core5x5.core.ui.components.AppPrimaryButton
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.components.AppWorkoutCard
import com.hulkdx.core5x5.core.ui.components.AppWorkoutTopBar
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** The detail layout is documented in design/specs/workout-detail.md. */
@Composable
internal fun WorkoutDetailScreen(
    uiState: WorkoutDetailUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    Surface(modifier = modifier.fillMaxSize(), color = AppColors.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppDimensions.ScreenInset,
                    top = AppDimensions.ContentGap,
                    end = AppDimensions.ScreenInset,
                    bottom = AppDimensions.ContentPaddingVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap),
        ) {
            AppWorkoutTopBar(
                title = uiState.completedWorkout?.displayName() ?: "Workout details",
                onBack = onBack,
                backContentDescription = "Back to History",
            )

            when {
                uiState.isLoading -> Text(
                    text = "Loading saved workout…",
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    style = AppTypography.Body,
                    color = AppColors.SecondaryText,
                    textAlign = TextAlign.Center,
                )
                uiState.completedWorkout != null -> CompletedWorkoutContent(
                    workout = uiState.completedWorkout,
                    weightUnit = weightUnit,
                )
                else -> DetailLoadError(
                    error = uiState.error ?: WorkoutDetailError.NOT_FOUND,
                    onRetryLoad = onRetryLoad,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun CompletedWorkoutContent(
    workout: CompletedWorkoutRecord,
    weightUnit: WeightUnit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.TextGap)) {
            Text(
                text = workout.detailDateLabel(),
                style = AppTypography.Body,
                color = AppColors.PrimaryText,
            )
            Text(
                text = "Saved completed session",
                style = AppTypography.Caption,
                color = AppColors.SecondaryText,
            )
        }
        AppMetrics(
            firstLabel = "Duration",
            firstValue = workout.durationClockLabel(),
            secondLabel = "Completed sets",
            secondValue = "${workout.completedSets} / ${workout.prescribedSets}",
        )
        AppWorkoutCard(
            eyebrow = "SAVED EXERCISES",
            title = workout.displayName(),
            indicator = workout.workout.name,
        ) {
            workout.exercises.forEach { exercise ->
                AppExerciseRow(
                    name = (exercise.customName ?: exercise.exercise.displayName()),
                    prescription = "${exercise.sets} × ${exercise.reps} · " +
                        "${formatWeight(exercise.weightKg, weightUnit)} · " +
                        "${exercise.completedSets} / ${exercise.sets} sets complete",
                    isCompleted = exercise.completedSets == exercise.sets,
                )
            }
        }
        Text(
            text = "Completed sets record the prescribed reps. Actual reps were not stored.",
            style = AppTypography.Caption,
            color = AppColors.SecondaryText,
        )
    }
}

@Composable
private fun DetailLoadError(
    error: WorkoutDetailError,
    onRetryLoad: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap),
    ) {
        Text(
            text = if (error == WorkoutDetailError.NOT_FOUND) {
                "This saved workout is no longer available."
            } else {
                "Unable to load this saved workout. Try again."
            },
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = AppTypography.Body,
            color = AppColors.SecondaryText,
            textAlign = TextAlign.Center,
        )
        AppPrimaryButton(label = "Try Again", onClick = onRetryLoad)
        AppSecondaryButton(label = "Back to History", onClick = onBack)
    }
}

@Preview(name = "Workout detail", widthDp = 390, heightDp = 844)
@Composable
private fun WorkoutDetailScreenPreview() {
    AppTheme {
        WorkoutDetailScreen(
            uiState = WorkoutDetailUiState(
                workoutId = 1L,
                isLoading = false,
                completedWorkout = previewCompletedWorkout(),
            ),
            onBack = {},
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Missing workout", widthDp = 320, heightDp = 640)
@Composable
private fun WorkoutDetailMissingPreview() {
    AppTheme {
        WorkoutDetailScreen(
            uiState = WorkoutDetailUiState(
                workoutId = 1L,
                isLoading = false,
                error = WorkoutDetailError.NOT_FOUND,
            ),
            onBack = {},
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun previewCompletedWorkout() = CompletedWorkoutRecord(
    id = 1L,
    workout = CompletedWorkoutType.A,
    startedAtEpochMillis = 1_727_251_200_000L,
    completedAtEpochMillis = 1_727_253_840_000L,
    exercises = listOf(
        previewExercise(CompletedExerciseType.SQUAT, 77.5),
        previewExercise(CompletedExerciseType.BENCH_PRESS, 45.0, completedSets = 4),
        previewExercise(CompletedExerciseType.BARBELL_ROW, 60.0),
    ),
)

private fun previewExercise(
    exercise: CompletedExerciseType,
    weightKg: Double,
    sets: Int = 5,
    completedSets: Int = sets,
) = CompletedWorkoutExercise(
    exercise = exercise,
    sets = sets,
    reps = 5,
    weightKg = weightKg,
    setStates = List(sets) { CompletedWorkoutSet(it, it < completedSets) },
)
