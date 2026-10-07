package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.Core5x5RestTokens as Rest

@Composable
fun Core5x5WorkoutSwitch(label: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(shape = RoundedCornerShape(Home.ControlRadius), color = Home.Card, border = BorderStroke(Home.BorderStroke, Home.Border)) {
            Row(
                modifier = Modifier.heightIn(min = Home.SwitchHeight).padding(horizontal = Home.SwitchPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Home.SwitchGap),
            ) {
                val color = if (enabled) Home.Action else Core5x5Colors.DisabledText
                Icon(imageVector = SwitchIcon, contentDescription = null, tint = color, modifier = Modifier.size(Home.IconSize))
                Text(text = label, style = Home.SwitchLabel, color = color)
            }
        }
    }
}

/** Informational prescription without a trailing action indicator. */
@Composable
fun Core5x5HomeExerciseRow(name: String, prescription: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = Home.RowHeight)
            .padding(horizontal = Home.TextInset, vertical = Home.RowPadding),
        verticalArrangement = Arrangement.spacedBy(Home.TextGap, Alignment.CenterVertically),
    ) {
        Text(text = name, style = Home.Exercise, color = Home.Primary)
        Text(text = prescription, style = Home.Prescription, color = Home.Secondary)
    }
}

@Composable
fun Core5x5HomeStartButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = Home.ButtonHeight),
        shape = RoundedCornerShape(Home.ControlRadius),
        contentPadding = PaddingValues(horizontal = Home.SwitchPadding),
        elevation = null,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) Home.ActionPressed else Home.Action,
            contentColor = Home.Background,
            disabledContainerColor = Core5x5Colors.Disabled,
            disabledContentColor = Core5x5Colors.DisabledText,
        ),
    ) {
        Text(text = label, style = Home.Button, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Core5x5RestFinishButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = Home.ButtonHeight),
        shape = RoundedCornerShape(Home.ControlRadius),
        border = BorderStroke(Home.BorderStroke, Home.Border),
        contentPadding = PaddingValues(horizontal = Home.SwitchPadding),
        elevation = null,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) Core5x5Colors.Pressed else Home.Card,
            contentColor = Home.Primary,
            disabledContainerColor = Core5x5Colors.Disabled,
            disabledContentColor = Core5x5Colors.DisabledText,
        ),
    ) {
        Text(text = label, style = Rest.FinishLabel,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private val SwitchIcon = homeStrokeIcon("Switch workout", "M4 8h15l-4-4M20 16H5l4 4M20 8v3M4 16v-3")

private fun homeStrokeIcon(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
    addPath(pathData = addPathNodes(path), fill = null, stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
}.build()
