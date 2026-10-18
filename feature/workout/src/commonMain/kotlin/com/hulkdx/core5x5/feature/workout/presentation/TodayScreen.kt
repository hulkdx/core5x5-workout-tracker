package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.ui.components.AppHomeExerciseRow
import com.hulkdx.core5x5.core.ui.components.AppHomeStartButton
import com.hulkdx.core5x5.core.ui.components.AppWorkoutSwitch
import com.hulkdx.core5x5.core.ui.components.WeightUnitError
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.feature.workout.domain.ExercisePrescription
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
    onStartWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    onSwitchWorkout: () -> Unit = {},
    weightUnit: WeightUnit = uiState.weightUnit,
    onRetryPreferences: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = Home.Background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = Home.TopGap, bottom = Home.BrandGap),
            verticalArrangement = Arrangement.spacedBy(Home.BrandGap),
        ) {
            if (uiState.hasPreferencesError) {
                Box(Modifier.fillMaxWidth().padding(horizontal = Home.Inset)) {
                    WeightUnitError(weightUnit, onRetryPreferences)
                }
            }
            Text(
                text = "Core5x5",
                style = Home.Brand,
                color = Home.Primary,
                modifier = Modifier.padding(horizontal = Home.BrandInset),
            )
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Home.Inset)) {
                val active = uiState.unfinishedWorkout
                val next = uiState.nextWorkout
                when {
                    uiState.isLoading && active == null && next == null -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Home.Action)
                        }
                    }
                    active != null -> WorkoutSummaryCard(
                        eyebrow = "WORKOUT IN PROGRESS",
                        workout = active.workout,
                        exercises = active.exercises.map { ExercisePrescription(it.exercise, it.sets, it.reps, it.weightKg) },
                        buttonLabel = "Resume Workout",
                        buttonEnabled = uiState.canResume,
                        error = uiState.error,
                        onAction = onResumeWorkout,
                        weightUnit = weightUnit,
                    )
                    next != null -> WorkoutSummaryCard(
                        eyebrow = "NEXT WORKOUT",
                        workout = next,
                        exercises = uiState.nextWorkoutPrescription?.exercises ?: WorkoutPrescription(next).exercises,
                        buttonLabel = if (uiState.isWorking && uiState.error == null) "Please wait…" else "Start Workout",
                        buttonEnabled = uiState.canStart,
                        error = uiState.error,
                        onAction = onStartWorkout,
                        onSwitch = onSwitchWorkout,
                        weightUnit = weightUnit,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(Home.BrandGap)) {
                        Text(text = "Unable to load your workout.", color = Home.Secondary, style = Home.Prescription)
                        AppHomeStartButton(label = "Try Again", onClick = onRetryLoad, enabled = !uiState.isLoading)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    eyebrow: String,
    workout: Workout,
    exercises: List<ExercisePrescription>,
    buttonLabel: String,
    buttonEnabled: Boolean,
    error: TodayError?,
    onAction: () -> Unit,
    weightUnit: WeightUnit,
    onSwitch: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Home.CardRadius),
        color = Home.Card,
        border = BorderStroke(Home.BorderStroke, Home.Border),
    ) {
        Column(modifier = Modifier.padding(Home.CardPadding)) {
            WorkoutHeading(eyebrow, workout, onSwitch, buttonEnabled)
            Spacer(Modifier.height(Home.HeadingGap))
            Divider()
            exercises.forEachIndexed { index, exercise ->
                AppHomeExerciseRow(
                    name = exercise.exercise.displayName(),
                    prescription = "${exercise.sets} × ${exercise.reps} · ${formatWeight(exercise.weightKg, weightUnit)}",
                )
                if (index != exercises.lastIndex) Divider()
            }
            Spacer(Modifier.height(Home.ButtonGap))
            if (error != null) {
                Text(text = error.message(), color = AppColors.Destructive, style = Home.Eyebrow)
                Spacer(Modifier.height(Home.ButtonGap))
            }
            AppHomeStartButton(label = buttonLabel, onClick = onAction, enabled = buttonEnabled)
        }
    }
}

@Composable
private fun WorkoutHeading(eyebrow: String, workout: Workout, onSwitch: (() -> Unit)?, enabled: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = Home.TextInset)) {
        // Keep both controls readable when the window narrows or the user enlarges text.
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Home.SwitchGap)) {
                HeadingText(eyebrow, workout)
                if (onSwitch != null) AppWorkoutSwitch("Switch to ${workout.nextWorkout().name}", onSwitch, enabled)
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Home.SwitchGap)) {
                HeadingText(eyebrow, workout, Modifier.weight(1f))
                if (onSwitch != null) AppWorkoutSwitch("Switch to ${workout.nextWorkout().name}", onSwitch, enabled)
            }
        }
    }
}

@Composable
private fun HeadingText(eyebrow: String, workout: Workout, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Home.TextGap)) {
        Text(text = eyebrow, style = Home.Eyebrow, color = Home.Secondary)
        Text(text = "Workout ${workout.name}", style = Home.Title, color = Home.Primary)
    }
}

@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = Home.TextInset),
    thickness = Home.BorderStroke,
    color = Home.Border,
)

private fun TodayError.message() = when (this) {
    TodayError.LOAD -> "Unable to refresh your workout. Try again."
    TodayError.START -> "Unable to start your workout. Try again."
    TodayError.SWITCH -> "Unable to switch workouts. Try again."
}

@Preview(name = "Today A", widthDp = 390, heightDp = 740)
@Preview(name = "Today narrow", widthDp = 320, heightDp = 540)
@Preview(name = "Today large text", widthDp = 320, heightDp = 540, fontScale = 2f)
@Composable
private fun TodayScreenPreview() {
    AppTheme {
        TodayScreen(TodayUiState(isLoading = false, nextWorkout = Workout.A), onStartWorkout = {}, onResumeWorkout = {}, onRetryLoad = {})
    }
}
