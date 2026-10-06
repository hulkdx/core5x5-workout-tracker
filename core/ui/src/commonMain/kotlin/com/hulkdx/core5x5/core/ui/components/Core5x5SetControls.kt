package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

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
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ContentGap),
    ) {
        sets.forEach { set ->
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
