package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.ui.components.AppButtonPalette
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens as History

@Composable
fun HistoryWeightGraph(
    workouts: List<CompletedWorkoutRecord>,
    selectedExercise: CompletedExerciseType,
    weightUnit: WeightUnit,
    onExerciseSelected: (CompletedExerciseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val points = remember(workouts, selectedExercise) { workouts.weightHistory(selectedExercise) }
    var selectedWorkoutId by remember(selectedExercise) { mutableStateOf<Long?>(null) }
    val selectedPoint = points.firstOrNull { it.workoutId == selectedWorkoutId }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(History.CardRadius),
        color = History.Card,
        border = BorderStroke(History.BorderStroke, History.Border),
    ) {
        Column(
            modifier = Modifier.padding(History.GraphPadding),
            verticalArrangement = Arrangement.spacedBy(History.GraphGap),
        ) {
            Text("Weight over time", Modifier.semantics { heading() }, style = History.Workout, color = History.Primary)
            ExerciseSelector(selectedExercise, onExerciseSelected)
            Text(
                "Saved weight in sessions with logged sets · All time",
                style = History.Metadata,
                color = History.Secondary,
            )
            if (points.isEmpty()) {
                Text(
                    "No logged sets for ${selectedExercise.displayName()} yet.",
                    style = History.Workout,
                    color = History.Secondary,
                )
            } else {
                val latest = points.last()
                Text(
                    "Latest: ${formatWeight(latest.weightKg, weightUnit)}",
                    style = History.Day,
                    color = History.Primary,
                )
                WeightPlot(
                    points = points,
                    exercise = selectedExercise,
                    unit = weightUnit,
                    selectedWorkoutId = selectedWorkoutId,
                    onPointSelected = { selectedWorkoutId = it },
                )
                Text(
                    text = selectedPoint?.let {
                        "Selected: ${formatWeight(it.weightKg, weightUnit)} · ${it.completedAtEpochMillis.toHistoryDateTime()}"
                    } ?: "Tap a point to see its recorded weight and date/time.",
                    modifier = Modifier.testTag("history-selected-point").semantics { liveRegion = LiveRegionMode.Polite },
                    style = History.Metadata,
                    color = History.Primary,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(History.GraphGap)) {
                    Text(
                        points.first().completedAtEpochMillis.toHistoryDetailDate(),
                        modifier = Modifier.weight(1f),
                        style = History.Metadata,
                        color = History.Secondary,
                    )
                    if (points.size > 1) {
                        Text(
                            latest.completedAtEpochMillis.toHistoryDetailDate(),
                            modifier = Modifier.weight(1f),
                            style = History.Metadata,
                            color = History.Secondary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
                if (points.size == 1) {
                    Text("Log another session to see a trend.", style = History.Metadata, color = History.Secondary)
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelector(selected: CompletedExerciseType, onSelected: (CompletedExerciseType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppSecondaryButton(
            label = "Lift: ${selected.displayName()}",
            modifier = Modifier.testTag("history-lift-selector"),
            onClick = { expanded = true },
            palette = AppButtonPalette.Home,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = History.Card) {
            CompletedExerciseType.entries.forEach { exercise ->
                DropdownMenuItem(
                    modifier = Modifier.testTag("history-lift:${exercise.name}").semantics { this.selected = exercise == selected },
                    text = { Text(exercise.displayName(), color = if (exercise == selected) History.Action else History.Primary) },
                    onClick = {
                        expanded = false
                        onSelected(exercise)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightPlot(
    points: List<HistoryWeightPoint>,
    exercise: CompletedExerciseType,
    unit: WeightUnit,
    selectedWorkoutId: Long?,
    onPointSelected: (Long) -> Unit,
) {
    val borderColor = History.Border
    val actionColor = History.Action
    // A zero baseline avoids exaggerating small weight changes and handles flat/zero histories.
    val maximum = points.maxOf { it.weightKg }.takeIf { it > 0.0 } ?: 1.0
    val description = remember(points, exercise, unit) {
        "${exercise.displayName()} weight history. " + points.joinToString("; ") {
            "${it.completedAtEpochMillis.toHistoryDateTime()}: ${formatWeight(it.weightKg, unit)}"
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(History.GraphGap)) {
        Column(Modifier.height(History.GraphHeight), verticalArrangement = Arrangement.SpaceBetween) {
            Text(formatWeight(maximum, unit), style = History.Metadata, color = History.Secondary)
            Text(formatWeight(0.0, unit), style = History.Metadata, color = History.Secondary)
        }
        Canvas(
            Modifier.weight(1f)
                .height(History.GraphHeight)
                .testTag("history-weight-plot")
                .pointerInput(points, selectedWorkoutId) {
                    detectTapGestures { position ->
                        val index = points.closestWeightPointIndex(
                            tapX = position.x,
                            tapY = position.y,
                            plotWidth = size.width.toFloat(),
                            plotHeight = size.height.toFloat(),
                            pointInset = History.GraphPointRadius.toPx(),
                            touchRadius = 24.dp.toPx(),
                            selectedWorkoutId = selectedWorkoutId,
                        )
                        if (index != null) onPointSelected(points[index].workoutId)
                    }
                }
                .semantics {
                    contentDescription = description
                    // An accessible action cycles through all saved points, even overlapping ones.
                    onClick(label = "Inspect next recorded weight") {
                        val current = points.indexOfFirst { it.workoutId == selectedWorkoutId }
                        onPointSelected(points[(current + 1) % points.size].workoutId)
                        true
                    }
                },
        ) {
            val inset = History.GraphPointRadius.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(0f)
            val height = (size.height - inset * 2).coerceAtLeast(0f)
            fun position(index: Int) = Offset(
                inset + points.timeFraction(index) * width,
                inset + (1.0 - points[index].weightKg / maximum).toFloat() * height,
            )
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = inset + fraction * height
                drawLine(borderColor, Offset(inset, y), Offset(inset + width, y), History.BorderStroke.toPx())
            }
            for (index in 1 until points.size) {
                drawLine(actionColor, position(index - 1), position(index), History.GraphStroke.toPx(), StrokeCap.Round)
            }
            points.indices.forEach { index ->
                drawCircle(
                    actionColor,
                    radius = if (points[index].workoutId == selectedWorkoutId) inset * 1.8f else inset,
                    center = position(index),
                )
            }
        }
    }
}
