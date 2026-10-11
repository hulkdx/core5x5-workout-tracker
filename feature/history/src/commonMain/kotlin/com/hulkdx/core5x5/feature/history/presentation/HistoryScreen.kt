package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import com.hulkdx.core5x5.core.ui.components.AppButtonPalette
import com.hulkdx.core5x5.core.ui.components.AppHistoryCard
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens as History
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** AppNavigation owns safe insets and bottom navigation; this screen owns its content layout. */
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val months = remember(uiState.completedWorkouts) { uiState.completedWorkouts.historyMonths() }
    Surface(modifier = modifier.fillMaxSize(), color = History.Background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = History.Inset,
                top = History.TopGap,
                end = History.Inset,
                bottom = History.BottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(History.CardGap),
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    text = "History",
                    modifier = Modifier.padding(bottom = History.TitleGap - History.CardGap).semantics { heading() },
                    style = History.Title,
                    color = History.Primary,
                )
            }
            when {
                uiState.isLoading && uiState.completedWorkouts.isEmpty() -> item(key = "loading") { LoadingHistory() }
                uiState.error != null && uiState.completedWorkouts.isEmpty() -> item(key = "error") { HistoryLoadError(onRetryLoad) }
                uiState.completedWorkouts.isEmpty() -> item(key = "empty") { EmptyHistory() }
                else -> {
                    if (uiState.error != null) {
                        item(key = "error") { HistoryLoadError(onRetryLoad) }
                    }
                    months.entries.forEachIndexed { index, (month, workouts) ->
                        item(key = "month:$month", contentType = "month") {
                            Text(
                                text = month,
                                modifier = Modifier
                                    .padding(
                                        top = if (index == 0) 0.dp else History.MonthGap - History.CardGap,
                                        bottom = History.MonthCardGap - History.CardGap,
                                    )
                                    .semantics { heading() },
                                style = History.Month,
                                color = History.Secondary,
                            )
                        }
                        items(workouts, key = { "workout:${it.id}" }, contentType = { "workout" }) { workout ->
                            AppHistoryCard(
                                weekday = workout.weekdayLabel(),
                                day = workout.dayLabel(),
                                fullDate = workout.detailDateLabel(),
                                title = workout.displayName(),
                                summary = workout.sessionSummary(),
                                duration = workout.durationLabel(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingHistory() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.ContentPaddingVertical),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = History.Action)
    }
}

@Composable
private fun EmptyHistory() {
    Text(
        text = "No completed workouts yet. Finish a workout to see it here.",
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        style = AppTypography.Body,
        color = History.Secondary,
    )
}

@Composable
private fun HistoryLoadError(onRetryLoad: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap),
    ) {
        Text(
            text = "Unable to load workout history. Try again.",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = AppTypography.Body,
            color = History.Secondary,
        )
        AppSecondaryButton(label = "Try Again", onClick = onRetryLoad, palette = AppButtonPalette.Home)
    }
}

@Preview(name = "History populated", widthDp = 390, heightDp = 874)
@Composable
private fun HistoryScreenPreview() {
    AppTheme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false, completedWorkouts = previewHistory()),
            onRetryLoad = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "History empty", widthDp = 320, heightDp = 640)
@Composable
private fun HistoryEmptyPreview() {
    AppTheme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false),
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
            previewExercise(CompletedExerciseType.OVERHEAD_PRESS, 37.5),
            previewExercise(CompletedExerciseType.DEADLIFT, 107.5, sets = 1),
        ),
    ),
    CompletedWorkoutRecord(
        id = 3L,
        workout = CompletedWorkoutType.A,
        startedAtEpochMillis = 1_727_251_200_000L,
        completedAtEpochMillis = 1_727_253_840_000L,
        exercises = listOf(
            previewExercise(CompletedExerciseType.SQUAT, 77.5),
            previewExercise(CompletedExerciseType.BENCH_PRESS, 45.0),
            previewExercise(CompletedExerciseType.BARBELL_ROW, 60.0),
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
