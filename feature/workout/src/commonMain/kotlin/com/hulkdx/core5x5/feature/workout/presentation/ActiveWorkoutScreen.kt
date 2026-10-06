package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hulkdx.core5x5.core.ui.components.Core5x5PrimaryButton
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise

@Composable
internal fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onBack: () -> Unit,
    onFinishWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Core5x5Dimensions.ScreenInset,
                    end = Core5x5Dimensions.ScreenInset,
                    top = Core5x5Dimensions.ContentGap,
                    bottom = Core5x5Dimensions.ContentPaddingVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ContentGap),
        ) {
            TextButton(onClick = onBack, enabled = !uiState.isSaving) {
                Text(text = "Today", color = Accent)
            }

            when {
                uiState.isLoading -> CircularProgressIndicator(color = Accent)
                uiState.hasLoadError -> {
                    Text(text = "Unable to load the active workout.", color = PrimaryText)
                    Core5x5PrimaryButton(label = "Try Again", onClick = onRetryLoad)
                }
                uiState.unfinishedWorkout != null -> {
                    ActiveWorkoutContent(uiState.unfinishedWorkout)
                    if (uiState.hasSaveError) {
                        Text(
                            text = "Unable to confirm the workout was saved. Try finishing again.",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = Core5x5Typography.Caption,
                            color = Core5x5Colors.Destructive,
                        )
                    }
                    Core5x5PrimaryButton(
                        label = if (uiState.isSaving) "Saving…" else "Finish Workout",
                        onClick = onFinishWorkout,
                        enabled = uiState.canFinish,
                    )
                }
                else -> Text(text = "No unfinished workout.", color = SecondaryText)
            }
        }
    }
}

@Composable
private fun ActiveWorkoutContent(workout: UnfinishedWorkout) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Workout ${workout.workout.name}",
            color = PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        workout.exercises.forEach { exercise -> ActiveExercise(exercise) }
    }
}

@Composable
private fun ActiveExercise(exercise: UnfinishedWorkoutExercise) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = exercise.exercise.displayName(),
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${exercise.sets} × ${exercise.reps} · ${exercise.weightKg.formatWeight()} kg",
            color = SecondaryText,
            fontSize = 14.sp,
        )
    }
}

private val Background = Color(0xFF0C1114)
private val PrimaryText = Color(0xFFF5F8F7)
private val SecondaryText = Color(0xFF9BA7AD)
private val Accent = Color(0xFF67E38B)
