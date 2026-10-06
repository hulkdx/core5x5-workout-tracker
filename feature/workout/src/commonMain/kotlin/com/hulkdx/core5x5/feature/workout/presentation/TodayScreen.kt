package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.ExercisePrescription
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout

@Composable
internal fun TodayScreen(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
    onStartWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                uiState.isLoading && uiState.nextWorkout == null && uiState.unfinishedWorkout == null -> {
                    LoadingContent()
                }

                uiState.unfinishedWorkout != null -> {
                    val workout = uiState.unfinishedWorkout
                    WorkoutSummaryCard(
                        eyebrow = "WORKOUT IN PROGRESS",
                        workout = workout.workout,
                        exercises = workout.exercises.map(UnfinishedWorkoutExercise::toSummary),
                        buttonLabel = if (uiState.isWorking) "Resuming…" else "Resume Workout",
                        buttonEnabled = uiState.canResume,
                        errorMessage = uiState.error?.message(),
                        onAction = onResumeWorkout,
                    )
                }

                uiState.nextWorkout != null -> {
                    val workout = uiState.nextWorkout
                    WorkoutSummaryCard(
                        eyebrow = "NEXT WORKOUT",
                        workout = workout,
                        exercises = uiState.nextWorkoutPrescription?.exercises?.map(ExercisePrescription::toSummary)
                            ?: workout.exercises.map(Exercise::toSummary),
                        buttonLabel = if (uiState.isWorking) "Starting…" else "Start Workout",
                        buttonEnabled = uiState.canStart,
                        errorMessage = uiState.error?.message(),
                        onAction = onStartWorkout,
                    )
                }

                else -> LoadError(onRetry = onRetryLoad)
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    eyebrow: String,
    workout: Workout,
    exercises: List<ExerciseSummary>,
    buttonLabel: String,
    buttonEnabled: Boolean,
    errorMessage: String?,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = eyebrow,
                        color = MutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Text(
                        text = "Workout ${workout.name}",
                        color = PrimaryText,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Accent,
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = workout.name,
                            color = Background,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            exercises.forEach { exercise -> ExerciseRow(exercise) }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = ErrorText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            Button(
                onClick = onAction,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Background,
                    disabledContainerColor = DisabledButton,
                    disabledContentColor = MutedText,
                ),
            ) {
                if (!buttonEnabled && buttonLabel.endsWith("…")) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Background,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = buttonLabel,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        color = RowSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exercise.name,
                color = PrimaryText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${exercise.sets} × ${exercise.reps} · ${exercise.weightLabel}",
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Unable to load your workout.",
            color = SecondaryText,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
        ) {
            Text(text = "Try Again")
        }
    }
}

private data class ExerciseSummary(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightLabel: String,
)

private fun Exercise.toSummary() = ExerciseSummary(
    name = displayName(),
    sets = sets,
    reps = reps,
    weightLabel = "${startingWeightKg.formatWeight()} kg",
)

private fun UnfinishedWorkoutExercise.toSummary() = ExerciseSummary(
    name = exercise.displayName(),
    sets = sets,
    reps = reps,
    weightLabel = "${weightKg.formatWeight()} kg",
)

private fun ExercisePrescription.toSummary() = ExerciseSummary(
    name = exercise.displayName(),
    sets = sets,
    reps = reps,
    weightLabel = "${weightKg.formatWeight()} kg",
)

private fun TodayError.message() = when (this) {
    TodayError.LOAD -> "Unable to refresh your workout. Try again."
    TodayError.START -> "Unable to start your workout. Try again."
    TodayError.RESUME -> "Unable to resume your workout. Try again."
}

@Preview
@Composable
private fun TodayScreenPreview() {
    Core5x5Theme {
        TodayScreen(
            uiState = TodayUiState(isLoading = false, nextWorkout = Workout.A),
            onStartWorkout = {},
            onResumeWorkout = {},
            onRetryLoad = {},
        )
    }
}

private val Background = Color(0xFF0C1114)
private val CardSurface = Color(0xFF151B20)
private val RowSurface = Color(0xFF11171B)
private val Accent = Color(0xFF67E38B)
private val PrimaryText = Color(0xFFF5F8F7)
private val SecondaryText = Color(0xFF9BA7AD)
private val MutedText = Color(0xFF68747A)
private val DisabledButton = Color(0xFF31463A)
private val ErrorText = Color(0xFFFF9C91)
