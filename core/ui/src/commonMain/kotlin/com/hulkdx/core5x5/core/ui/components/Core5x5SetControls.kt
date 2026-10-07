package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.theme.Core5x5ActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
import com.hulkdx.core5x5.core.ui.theme.Core5x5RestTokens as Rest

enum class Core5x5SetState { Default, Active, Completed, Disabled }

data class Core5x5SetControl(
    val position: Int,
    val state: Core5x5SetState,
    val completionDescription: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Core5x5SetControls(
    sets: List<Core5x5SetControl>,
    onComplete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    refined: Boolean = false,
    enabled: Boolean = true,
    resting: Boolean = false,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ContentGap),
    ) {
        sets.forEach { set ->
            if (refined) {
                RefinedSetControl(set, enabled, resting, onComplete)
                return@forEach
            }
            val active = set.state == Core5x5SetState.Active
            val completed = set.state == Core5x5SetState.Completed
            val disabled = set.state == Core5x5SetState.Disabled
            val size = maxOf(
                Core5x5Dimensions.SetDiameter,
                with(LocalDensity.current) { Core5x5Typography.Label.lineHeight.toDp() },
            )
            val background = when (set.state) {
                Core5x5SetState.Default -> Core5x5Colors.Elevated
                Core5x5SetState.Active -> Core5x5Colors.ActionTint
                Core5x5SetState.Completed -> Core5x5Colors.Action
                Core5x5SetState.Disabled -> Core5x5Colors.Disabled
            }
            val foreground = when (set.state) {
                Core5x5SetState.Default -> Core5x5Colors.SecondaryText
                Core5x5SetState.Active -> Core5x5Colors.Action
                Core5x5SetState.Completed -> Core5x5Colors.Background
                Core5x5SetState.Disabled -> Core5x5Colors.DisabledText
            }
            Surface(
                onClick = { onComplete(set.position) },
                modifier = Modifier.sizeIn(minWidth = size, minHeight = size).semantics {
                    contentDescription = set.completionDescription
                    stateDescription = when {
                        completed -> "Completed"
                        active -> "Next set"
                        disabled -> "Saving"
                        else -> "Incomplete"
                    }
                },
                enabled = !completed && !disabled,
                shape = CircleShape,
                color = background,
                contentColor = foreground,
                border = if (active) BorderStroke(Core5x5Dimensions.SetActiveStroke, Core5x5Colors.Action) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (completed) "✓" else (set.position + 1).toString(),
                        modifier = Modifier.clearAndSetSemantics {},
                        style = Core5x5Typography.Label,
                    )
                }
            }
        }
    }
}

/** Active's smaller circles and Rest's larger circles share non-overlapping minimum 48dp targets. */
@Composable
private fun RefinedSetControl(set: Core5x5SetControl, enabled: Boolean, resting: Boolean, onComplete: (Int) -> Unit) {
    val completed = set.state == Core5x5SetState.Completed
    val current = set.state == Core5x5SetState.Active
    val disabled = set.state == Core5x5SetState.Disabled || !enabled
    val labelStyle = if (resting) Rest.SetLabel else Active.SetLabel
    val visibleSize = maxOf(
        if (resting) Rest.SetDiameter else Active.SetDiameter,
        with(LocalDensity.current) { labelStyle.lineHeight.toDp() },
    )
    val targetSize = maxOf(Core5x5Dimensions.TouchTargetMin, visibleSize)
    Box(
        modifier = Modifier.size(targetSize)
            .clickable(enabled = !completed && !disabled, role = Role.Button) { onComplete(set.position) }
            .semantics {
                contentDescription = set.completionDescription
                stateDescription = when {
                    completed -> "Completed"
                    current -> "Next set"
                    disabled -> "Saving"
                    else -> "Incomplete"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(visibleSize),
            shape = CircleShape,
            color = when {
                completed -> Home.Action
                current -> Home.Card
                else -> Active.SetSurface
            },
            border = when {
                completed -> null
                current -> BorderStroke(Active.SetActiveStroke, Home.Action)
                else -> BorderStroke(Home.BorderStroke, Home.Border)
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) Icon(
                    imageVector = CheckIcon,
                    contentDescription = null,
                    tint = Home.Background,
                    modifier = Modifier.size(Home.IconSize),
                ) else Text(
                    text = (set.position + 1).toString(),
                    modifier = Modifier.clearAndSetSemantics {},
                    style = labelStyle,
                    color = when {
                        completed -> Home.Background
                        current -> Home.Primary
                        else -> Home.Secondary
                    },
                )
            }
        }
    }
}

private val CheckIcon = ImageVector.Builder(
    name = "Completed set", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
    addPath(pathData = addPathNodes("M5 12l5 5L20 7"), fill = null, stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
}.build()
