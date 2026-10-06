package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
fun Core5x5WorkoutCard(
    eyebrow: String,
    title: String,
    indicator: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusLarge),
        color = Core5x5Colors.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(Core5x5Dimensions.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.CardGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.CardHeadingMinHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = eyebrow, style = Core5x5Typography.Caption, color = Core5x5Colors.MutedText)
                    Text(text = title, style = Core5x5Typography.Heading, color = Core5x5Colors.PrimaryText)
                }
                Core5x5WorkoutIndicator(indicator)
            }
            content()
        }
    }
}

@Composable
fun Core5x5WorkoutIndicator(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusSmall),
        color = Core5x5Colors.Action,
    ) {
        Box(
            modifier = Modifier.sizeIn(
                minWidth = Core5x5Dimensions.IndicatorSize,
                minHeight = Core5x5Dimensions.IndicatorSize,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = Core5x5Typography.Label, color = Core5x5Colors.Background)
        }
    }
}

/** An informational row or a single exercise-selection target; its status is decorative. */
@Composable
fun Core5x5ExerciseRow(
    name: String,
    prescription: String,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val interactionModifier = if (onClick == null) Modifier else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics {
            stateDescription = if (isCompleted) "Completed" else "Incomplete"
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.ExerciseRowHeight).then(interactionModifier),
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusMedium),
        color = Core5x5Colors.Subtle,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Core5x5Dimensions.ExerciseRowInset,
                vertical = Core5x5Dimensions.ExerciseRowPaddingVertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(Core5x5Dimensions.ExerciseRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.TextGap),
            ) {
                Text(text = name, style = Core5x5Typography.Label, color = Core5x5Colors.PrimaryText)
                Text(
                    text = prescription,
                    style = Core5x5Typography.Caption,
                    color = if (isCompleted) Core5x5Colors.MutedText else Core5x5Colors.SecondaryText,
                )
            }
            val statusSize = maxOf(
                Core5x5Dimensions.ExerciseStatusSize,
                with(LocalDensity.current) { Core5x5Typography.Heading.lineHeight.toDp() },
            )
            Surface(
                shape = CircleShape,
                color = if (isCompleted) Core5x5Colors.ActionTint else Core5x5Colors.Elevated,
            ) {
                Box(
                    modifier = Modifier.size(statusSize).clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isCompleted) "✓" else "›",
                        style = Core5x5Typography.Heading,
                        color = if (isCompleted) Core5x5Colors.Action else Core5x5Colors.SecondaryText,
                    )
                }
            }
        }
    }
}
