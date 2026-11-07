package com.hulkdx.core5x5.feature.history.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.ui.components.AppHistoryCard
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens as History
import kotlin.math.roundToInt

/** Reveal state belongs to the list so dragging another row closes the previous one. */
@Composable
fun HistorySessionRow(
    workout: CompletedWorkoutRecord,
    revealed: Boolean,
    enabled: Boolean,
    isDeleting: Boolean,
    onDragStarted: () -> Unit,
    onReveal: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val actionWidth = with(LocalDensity.current) { History.DeleteActionWidth.toPx() }
    val animation = remember(workout.id) { Animatable(0f) }
    var dragging by remember(workout.id) { mutableStateOf(false) }
    var dragOffset by remember(workout.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(revealed, enabled, dragging, actionWidth) {
        if (!dragging) animation.animateTo(if (revealed && enabled) -actionWidth else 0f)
    }
    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceIn(-actionWidth, 0f)
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(History.CardRadius))) {
        Row(
            modifier = Modifier.matchParentSize().background(AppColors.Destructive),
            horizontalArrangement = Arrangement.Absolute.Right,
        ) {
            // Covered actions are decorative until the swipe has settled open.
            val actionSemantics = if (revealed && enabled) Modifier.semantics {
                contentDescription = "Delete ${workout.displayName()}, ${workout.detailDateLabel()}"
            } else Modifier.clearAndSetSemantics {}
            TextButton(
                onClick = onDelete,
                enabled = revealed && enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = Color.White),
                modifier = Modifier.width(History.DeleteActionWidth).fillMaxHeight()
                    .then(actionSemantics).testTag("history-swipe-delete:${workout.id}"),
            ) {
                Text("Delete")
            }
        }
        Box(
            Modifier.absoluteOffset { IntOffset((if (dragging) dragOffset else animation.value).roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStarted = {
                        animation.stop()
                        dragOffset = animation.value
                        dragging = true
                        onDragStarted()
                    },
                    onDragStopped = {
                        animation.snapTo(dragOffset)
                        dragging = false
                        // Swiping never deletes, regardless of travel or release velocity.
                        onReveal(dragOffset < -actionWidth / 2f)
                    },
                ),
        ) {
            AppHistoryCard(
                modifier = Modifier.testTag("history-workout:${workout.id}").semantics {
                    stateDescription = when {
                        isDeleting -> "Deleting workout"
                        revealed -> "Delete revealed"
                        else -> "Delete hidden"
                    }
                    customActions = if (enabled) listOf(
                        CustomAccessibilityAction(if (revealed) "Hide Delete" else "Show Delete") {
                            onReveal(!revealed)
                            true
                        },
                    ) else emptyList()
                },
                weekday = workout.weekdayLabel(),
                day = workout.dayLabel(),
                fullDate = workout.detailDateLabel(),
                title = workout.displayName(),
                summary = workout.sessionSummary(),
                duration = workout.durationLabel(),
                trailingContent = {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).clearAndSetSemantics {},
                            color = History.Action,
                        )
                    }
                },
            )
        }
    }
}
