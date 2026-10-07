package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.theme.Core5x5EditTokens as Edit
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun Core5x5EditCard(modifier: Modifier = Modifier, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Home.Card, shape = RoundedCornerShape(Home.CardRadius),
        border = BorderStroke(Home.BorderStroke, Home.Border)) {
        Column(Modifier.padding(horizontal = Edit.CardPadding, vertical = if (compact) 6.dp else Edit.CardPadding), content = content)
    }
}

@Composable
fun Core5x5EditStepper(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit,
    enabled: Boolean, canDecrease: Boolean = true, canIncrease: Boolean = true) {
    BoxWithConstraints {
        val stack = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        val controls: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton("−", "Decrease $label", onDecrease, enabled && canDecrease)
                Text(value, Modifier.widthIn(min = 52.dp).padding(horizontal = 8.dp), color = Home.Primary,
                    style = Edit.Value, textAlign = TextAlign.Center)
                StepperButton("+", "Increase $label", onIncrease, enabled && canIncrease)
            }
        }
        if (stack) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = Edit.Body, color = Home.Secondary)
            controls()
        } else Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = Edit.Body, color = Home.Secondary)
            controls()
        }
    }
}

@Composable
private fun StepperButton(text: String, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(Modifier.size(48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Surface(Modifier.size(34.dp), shape = CircleShape, color = Edit.Control) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (text == "+") PlusIcon else MinusIcon, contentDescription = null,
                    tint = if (enabled) Home.Secondary else Home.Secondary.copy(alpha = .35f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Core5x5RestChoices(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        listOf("Off", "3 min", "Custom").forEachIndexed { index, label ->
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(Home.ControlRadius), color = Home.Card,
                border = BorderStroke(Home.BorderStroke, if (selected == index) Home.Action else Home.Border)) {
                TextButton(onClick = { onSelect(index) }, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(label, style = Edit.Body, color = if (selected == index) Home.Action else Home.Secondary,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Snap-to-center picker; selected values remain available to accessibility services as buttons. */
@Composable
fun Core5x5DurationWheel(label: String, count: Int, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, padZero: Boolean = false, onScrolling: (Boolean) -> Unit = {}) {
    val itemHeight = with(LocalDensity.current) { Edit.Picker.lineHeight.toDp().coerceAtLeast(28.dp) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, count - 1))
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnScrolling by rememberUpdatedState(onScrolling)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            currentOnScrolling(scrolling)
            if (!scrolling) {
                val center = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
                state.layoutInfo.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
                    ?.let { currentOnSelect(it.index) }
            }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Edit.Body, color = Home.Secondary)
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxWidth().height(itemHeight), shape = RoundedCornerShape(Home.CardRadius), color = Edit.Control) {}
            LazyColumn(state = state, modifier = Modifier.fillMaxWidth().height(itemHeight * 5),
                contentPadding = PaddingValues(vertical = itemHeight * 2),
                flingBehavior = rememberSnapFlingBehavior(state)) {
                items(count) { value ->
                    val distance = kotlin.math.abs(value - selected)
                    Box(Modifier.fillMaxWidth().height(itemHeight)
                        .clickable(role = Role.Button, onClick = { currentOnSelect(value) })
                        .semantics { contentDescription = "$value $label" }, contentAlignment = Alignment.Center) {
                        Text(if (padZero) value.toString().padStart(2, '0') else value.toString(),
                            style = Edit.Picker, color = Home.Primary.copy(alpha = if (distance == 0) 1f else .45f))
                    }
                }
            }
        }
    }
    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selected) state.scrollToItem(selected)
    }
}

@Composable
fun Core5x5EditCloseIcon() = Icon(CloseIcon, contentDescription = null, tint = Home.Secondary, modifier = Modifier.size(24.dp))

@Composable
fun Core5x5EditTimerIcon() = Icon(TimerIcon, contentDescription = null, tint = Home.Secondary, modifier = Modifier.size(20.dp))

private fun editIcon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    addPath(pathData = addPathNodes(path), fill = null, stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round)
}.build()
private val PlusIcon = editIcon("Increase", "M5 12h14M12 5v14")
private val MinusIcon = editIcon("Decrease", "M5 12h14")
private val CloseIcon = editIcon("Close", "M5 5l14 14M19 5L5 19")
private val TimerIcon = editIcon("Rest timer", "M9 2h6M12 2v3M18 5l2 2M12 9v5M21 14a9 9 0 1 1-18 0a9 9 0 1 1 18 0")
