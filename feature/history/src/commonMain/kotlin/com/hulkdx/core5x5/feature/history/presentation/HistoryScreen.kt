package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import com.hulkdx.core5x5.core.ui.components.Core5x5HistoryCard
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

/** AppNavigation owns safe insets and bottom navigation; this screen owns its content layout. */
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onWorkoutSelected: (Long) -> Unit,
    onRetryLoad: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Core5x5Colors.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Core5x5Dimensions.ScreenInset,
                    top = Core5x5Dimensions.ContentGap,
                    end = Core5x5Dimensions.ScreenInset,
                    bottom = Core5x5Dimensions.ContentPaddingVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ContentGap),
        ) {
            Text(
                text = "History",
                modifier = Modifier.semantics { heading() },
                style = Core5x5Typography.Title,
                color = Core5x5Colors.PrimaryText,
            )
            Text(
                text = "Past sessions, newest first.",
                style = Core5x5Typography.Body,
                color = Core5x5Colors.SecondaryText,
            )

            when {
                uiState.isLoading && uiState.completedWorkouts.isEmpty() -> LoadingHistory()
                uiState.error != null && uiState.completedWorkouts.isEmpty() -> HistoryLoadError(onRetryLoad)
                uiState.completedWorkouts.isEmpty() -> EmptyHistory()
                else -> {
                    if (uiState.error != null) {
                        HistoryLoadError(onRetryLoad)
                    }
                    uiState.completedWorkouts.forEach { workout ->
                        Core5x5HistoryCard(
                            badge = workout.workout.name,
                            title = workout.displayName(),
                            dateAndDuration = "${workout.dateLabel()} · ${workout.durationLabel()}",
                            summary = workout.exerciseSummary(weightUnit),
                            onClick = { onWorkoutSelected(workout.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingHistory() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Core5x5Dimensions.ContentPaddingVertical),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Core5x5Colors.Action)
    }
}

@Composable
private fun EmptyHistory() {
    Text(
        text = "No completed workouts yet. Finish a workout to see it here.",
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        style = Core5x5Typography.Body,
        color = Core5x5Colors.SecondaryText,
    )
}

@Composable
private fun HistoryLoadError(onRetryLoad: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ContentGap),
    ) {
        Text(
            text = "Unable to load workout history. Try again.",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = Core5x5Typography.Body,
            color = Core5x5Colors.SecondaryText,
        )
        Core5x5SecondaryButton(label = "Try Again", onClick = onRetryLoad)
    }
}

@Preview(name = "History populated", widthDp = 390, heightDp = 844)
@Composable
private fun HistoryScreenPreview() {
    Core5x5Theme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false, completedWorkouts = previewHistory()),
            onWorkoutSelected = {},
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "History empty", widthDp = 320, heightDp = 640)
@Composable
private fun HistoryEmptyPreview() {
    Core5x5Theme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false),
            onWorkoutSelected = {},
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun previewHistory() = listOf(
    CompletedWorkoutRecord(
        id = 4L,
        workout = CompletedWorkoutType.B,
        startedAtEpochMillis = 1_727_856_000_000L,
        completedAtEpochMillis = 1_727_858_460_000L,
        exercises = listOf(
            previewExercise(CompletedExerciseType.SQUAT, 77.5),
            previewExercise(CompletedExerciseType.DEADLIFT, 107.5, sets = 1),
            previewExercise(CompletedExerciseType.OVERHEAD_PRESS, 37.5),
        ),
    ),
    CompletedWorkoutRecord(
        id = 3L,
        workout = CompletedWorkoutType.A,
        startedAtEpochMillis = 1_727_251_200_000L,
        completedAtEpochMillis = 1_727_253_840_000L,
        exercises = listOf(
            previewExercise(CompletedExerciseType.SQUAT, 77.5),
            previewExercise(CompletedExerciseType.BARBELL_ROW, 60.0),
            previewExercise(CompletedExerciseType.BENCH_PRESS, 45.0),
        ),
    ),
)

private fun previewExercise(
    exercise: CompletedExerciseType,
    weightKg: Double,
    sets: Int = 5,
) = CompletedWorkoutExercise(
    exercise = exercise,
    sets = sets,
    reps = 5,
    weightKg = weightKg,
    setStates = List(sets) { CompletedWorkoutSet(it, true) },
)
