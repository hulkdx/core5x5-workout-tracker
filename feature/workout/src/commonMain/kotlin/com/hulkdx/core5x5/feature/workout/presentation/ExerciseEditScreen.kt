package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.formatWeight
import com.hulkdx.core5x5.core.ui.components.*
import com.hulkdx.core5x5.core.ui.theme.Core5x5EditTokens as Edit
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseEditScreen(
    state: ExerciseEditUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChanged: (String) -> Unit,
    onWeightChanged: (Int) -> Unit,
    onSetsChanged: (Int) -> Unit,
    onRepsChanged: (Int) -> Unit,
    onRestSelected: (Long) -> Unit,
    onOpenCustomRest: () -> Unit,
    onCustomRestChanged: (Long) -> Unit,
    onApplyCustomRest: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val enabled = !state.isSaving
    Surface(modifier.fillMaxSize(), color = Home.Background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(windowInsets).imePadding()
            .padding(horizontal = Edit.Inset, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.size(48.dp).semantics { contentDescription = "Cancel exercise editing" }) {
                    Core5x5EditCloseIcon()
                }
                Text("Edit ${state.originalName}", Modifier.weight(1f), style = Edit.Title,
                    color = Home.Primary, textAlign = TextAlign.Center)
                TextButton(onClick = onSave, enabled = state.canSave) {
                    Text(if (state.isSaving) "Saving…" else "Save", style = Edit.Value,
                        color = if (state.canSave) Home.Action else Home.Secondary)
                }
            }
            Text("Adjust the details for this exercise.", Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
                style = Edit.Body, color = Home.Secondary, textAlign = TextAlign.Center)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Edit.Gap)) {
                Core5x5EditCard {
                    Text("Exercise Name", style = Edit.Body, color = Home.Secondary)
                    Spacer(Modifier.height(Edit.NameGap))
                    OutlinedTextField(value = state.name, onValueChange = onNameChanged, enabled = enabled,
                        modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = Edit.Value,
                        shape = RoundedCornerShape(Home.ControlRadius),
                        trailingIcon = {
                            TextButton(onClick = { onNameChanged("") }, enabled = enabled && state.name.isNotEmpty(),
                                modifier = Modifier.semantics { contentDescription = "Clear exercise name" }) {
                                Core5x5EditCloseIcon()
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Home.Action,
                            unfocusedBorderColor = Edit.FieldBorder, focusedTextColor = Home.Primary,
                            unfocusedTextColor = Home.Primary, cursorColor = Home.Action),
                    )
                }
                Core5x5EditCard(compact = true) {
                    Core5x5EditStepper("Working Weight (${state.unit.symbol})",
                        formatWeight(state.weightKg, state.unit).removeSuffix(" ${state.unit.symbol}"),
                        { onWeightChanged(-1) }, { onWeightChanged(1) }, enabled,
                        canDecrease = state.weightKg > 0, canIncrease = state.weightKg < 1_000_000)
                }
                Core5x5EditCard(compact = true) {
                    Core5x5EditStepper("Sets", state.sets.toString(), { onSetsChanged(-1) }, { onSetsChanged(1) },
                        enabled, canDecrease = state.sets > state.minimumSets, canIncrease = state.sets < 100)
                }
                Core5x5EditCard(compact = true) {
                    Core5x5EditStepper("Reps", state.reps.toString(), { onRepsChanged(-1) }, { onRepsChanged(1) },
                        enabled, canDecrease = state.reps > 1, canIncrease = state.reps < 100)
                }
                Core5x5EditCard(compact = true) {
                    val restChoices: @Composable () -> Unit = {
                    Core5x5RestChoices(selected = if (state.showCustomRest) 2 else when (state.effectiveRestDurationMillis) {
                        0L -> 0
                        180_000L -> 1
                        else -> 2
                    }, enabled = enabled, onSelect = { choice ->
                        when (choice) {
                            0 -> onRestSelected(0)
                            1 -> onRestSelected(180_000)
                            else -> onOpenCustomRest()
                        }
                    })
                    }
                    BoxWithConstraints {
                        if (maxWidth >= 320.dp && LocalDensity.current.fontScale <= 1.3f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.width(116.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Core5x5EditTimerIcon()
                                    Text("Rest Timer", style = Edit.Body, color = Home.Secondary)
                                }
                                Box(Modifier.weight(1f)) { restChoices() }
                            }
                        } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rest Timer", style = Edit.Body, color = Home.Secondary)
                            restChoices()
                        }
                    }
                    if (state.effectiveRestDurationMillis !in listOf(0L, 180_000L)) {
                        Text(restDurationLabel(state.effectiveRestDurationMillis / 1_000),
                            Modifier.padding(top = 6.dp), style = Edit.Body, color = Home.Secondary)
                    }
                }
                if (state.name.isBlank()) EditError("Enter an exercise name.")
                if (state.hasSaveError) EditError("Unable to save changes. Try again.")
                Spacer(Modifier.height(Edit.Gap))
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)) {
                Core5x5HomeStartButton(if (state.isSaving) "Saving…" else "Save Changes", onSave, state.canSave)
            }
        }
    }
    if (state.showCustomRest) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Edit.Sheet, contentColor = Home.Primary, scrimColor = Color.Black.copy(alpha = .15f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            CustomRestContent(state.customRestSeconds, onCustomRestChanged, onDismiss, onApplyCustomRest)
        }
    }
}

@Composable
internal fun CustomRestContent(seconds: Long, onChanged: (Long) -> Unit, onCancel: () -> Unit, onApply: () -> Unit) {
    var minutesScrolling by remember { mutableStateOf(false) }
    var secondsScrolling by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Edit.Inset, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Custom Rest Timer", style = Edit.Title, color = Home.Primary, textAlign = TextAlign.Center)
        Text("Choose the rest duration for this exercise.", style = Edit.Body, color = Home.Secondary, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (seconds / 60 >= Int.MAX_VALUE) {
                OutlinedTextField((seconds / 60).toString(), onValueChange = { input ->
                    input.toLongOrNull()?.takeIf { it in 0..(Long.MAX_VALUE / 1_000 - seconds % 60) / 60 }
                        ?.let { onChanged(it * 60 + seconds % 60) }
                }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true)
            } else Core5x5DurationWheel("Minutes", count = ((seconds / 60 + 1).coerceAtLeast(100).coerceAtMost(Int.MAX_VALUE.toLong())).toInt(),
                selected = (seconds / 60).coerceAtMost(Int.MAX_VALUE.toLong() - 1).toInt(),
                onSelect = { onChanged(it * 60L + seconds % 60) }, modifier = Modifier.weight(1f),
                onScrolling = { minutesScrolling = it })
            Core5x5DurationWheel("Seconds", count = 60, selected = (seconds % 60).toInt(),
                onSelect = { onChanged(seconds / 60 * 60 + it) }, modifier = Modifier.weight(1f), padZero = true,
                onScrolling = { secondsScrolling = it })
        }
        Surface(shape = RoundedCornerShape(50), color = Edit.Sheet, border = BorderStroke(Home.BorderStroke, Home.Border)) {
            Text("Selected: ${restDurationLabel(seconds)}", Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = Edit.Value, color = Home.Primary, textAlign = TextAlign.Center)
        }
        if (seconds == 0L) EditError("Choose a positive duration, or use Off in the editor.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Core5x5RestFinishButton("Cancel", onCancel, true) }
            Box(Modifier.weight(1f)) { Core5x5HomeStartButton("Apply", onApply, seconds > 0 && !minutesScrolling && !secondsScrolling) }
        }
    }
}

internal fun restDurationLabel(seconds: Long): String = "${seconds / 60} min ${seconds % 60} sec"

@Composable
private fun EditError(message: String) {
    Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = Edit.Body,
        color = com.hulkdx.core5x5.core.ui.theme.Core5x5Colors.Destructive)
}
