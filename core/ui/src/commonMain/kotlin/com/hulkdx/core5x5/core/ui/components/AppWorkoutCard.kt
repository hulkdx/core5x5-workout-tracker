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
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTypography

@Composable
fun AppWorkoutCard(
    eyebrow: String,
    title: String,
    indicator: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.RadiusLarge),
        color = AppColors.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.CardPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.CardGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.CardHeadingMinHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = eyebrow, style = AppTypography.Caption, color = AppColors.MutedText)
                    Text(text = title, style = AppTypography.Heading, color = AppColors.PrimaryText)
                }
                AppWorkoutIndicator(indicator)
            }
            content()
        }
    }
}

@Composable
fun AppWorkoutIndicator(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.RadiusSmall),
        color = AppColors.Action,
    ) {
        Box(
            modifier = Modifier.sizeIn(
                minWidth = AppDimensions.IndicatorSize,
                minHeight = AppDimensions.IndicatorSize,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = AppTypography.Label, color = AppColors.Background)
        }
    }
}

/** An informational row or a single exercise-selection target; its status is decorative. */
@Composable
fun AppExerciseRow(
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
        modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.ExerciseRowHeight).then(interactionModifier),
        shape = RoundedCornerShape(AppDimensions.RadiusMedium),
        color = AppColors.Subtle,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimensions.ExerciseRowInset,
                vertical = AppDimensions.ExerciseRowPaddingVertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.ExerciseRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.TextGap),
            ) {
                Text(text = name, style = AppTypography.Label, color = AppColors.PrimaryText)
                Text(
                    text = prescription,
                    style = AppTypography.Caption,
                    color = if (isCompleted) AppColors.MutedText else AppColors.SecondaryText,
                )
            }
            val statusSize = maxOf(
                AppDimensions.ExerciseStatusSize,
                with(LocalDensity.current) { AppTypography.Heading.lineHeight.toDp() },
            )
            Surface(
                shape = CircleShape,
                color = if (isCompleted) AppColors.ActionTint else AppColors.Elevated,
            ) {
                Box(
                    modifier = Modifier.size(statusSize).clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isCompleted) "✓" else "›",
                        style = AppTypography.Heading,
                        color = if (isCompleted) AppColors.Action else AppColors.SecondaryText,
                    )
                }
            }
        }
    }
}
