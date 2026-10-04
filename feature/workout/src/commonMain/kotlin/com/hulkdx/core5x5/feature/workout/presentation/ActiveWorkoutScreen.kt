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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise

@Composable
internal fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(text = "Today", color = Accent)
            }

            when {
                uiState.isLoading -> CircularProgressIndicator(color = Accent)
                uiState.hasLoadError -> Text(
                    text = "Unable to load the active workout.",
                    color = PrimaryText,
                )
                uiState.unfinishedWorkout != null -> ActiveWorkoutContent(uiState.unfinishedWorkout)
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

private fun Exercise.displayName() = when (this) {
    Exercise.SQUAT -> "Squat"
    Exercise.BENCH_PRESS -> "Bench Press"
    Exercise.BARBELL_ROW -> "Barbell Row"
    Exercise.OVERHEAD_PRESS -> "Overhead Press"
    Exercise.DEADLIFT -> "Deadlift"
}

private fun Double.formatWeight() = if (this % 1.0 == 0.0) toInt().toString() else toString()

private val Background = Color(0xFF0C1114)
private val PrimaryText = Color(0xFFF5F8F7)
private val SecondaryText = Color(0xFF9BA7AD)
private val Accent = Color(0xFF67E38B)
