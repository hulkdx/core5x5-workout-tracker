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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.ui.components.AppButtonPalette
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.components.AppWorkoutTopBar
import com.hulkdx.core5x5.core.ui.components.WeightUnitError
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens as History
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** Focused graph screen owns safe insets and intentionally hides the bottom navigation. */
@Composable
fun HistoryGraphScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onExerciseSelected: (CompletedExerciseType) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    onRetryPreferences: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = History.Background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .verticalScroll(rememberScrollState())
                .testTag("history-graph-screen")
                .padding(
                    start = History.Inset,
                    top = History.TopGap,
                    end = History.Inset,
                    bottom = History.BottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap),
        ) {
            AppWorkoutTopBar(
                title = "Weight graph",
                onBack = onBack,
                backContentDescription = "Back to History",
                refined = true,
            )
            if (uiState.hasPreferencesError) WeightUnitError(uiState.weightUnit, onRetryPreferences)
            when {
                uiState.isLoading && uiState.completedWorkouts.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = History.Action)
                }
                uiState.error != null && uiState.completedWorkouts.isEmpty() -> GraphLoadError(onRetryLoad)
                else -> {
                    if (uiState.error != null) GraphLoadError(onRetryLoad)
                    HistoryWeightGraph(
                        workouts = uiState.completedWorkouts,
                        selectedExercise = uiState.selectedExercise,
                        weightUnit = uiState.weightUnit,
                        onExerciseSelected = onExerciseSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphLoadError(onRetryLoad: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap)) {
        Text(
            "Unable to load weight history. Try again.",
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = AppTypography.Body,
            color = History.Secondary,
        )
        AppSecondaryButton(label = "Try Again", onClick = onRetryLoad, palette = AppButtonPalette.Home)
    }
}
