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
import com.hulkdx.core5x5.core.ui.theme.AppActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppTypography
import com.hulkdx.core5x5.core.ui.theme.AppRestTokens as Rest

enum class AppSetState { Default, Active, Completed, Disabled }

data class AppSetControl(
    val position: Int,
    val state: AppSetState,
    val completionDescription: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSetControls(
    sets: List<AppSetControl>,
    onComplete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    refined: Boolean = false,
    enabled: Boolean = true,
    resting: Boolean = false,
    allowUndo: Boolean = false,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.ContentGap),
    ) {
        sets.forEach { set ->
            if (refined) {
                RefinedSetControl(set, enabled, resting, allowUndo, onComplete)
                return@forEach
            }
            val active = set.state == AppSetState.Active
            val completed = set.state == AppSetState.Completed
            val disabled = set.state == AppSetState.Disabled
            val size = maxOf(
                AppDimensions.SetDiameter,
                with(LocalDensity.current) { AppTypography.Label.lineHeight.toDp() },
            )
            val background = when (set.state) {
                AppSetState.Default -> AppColors.Elevated
                AppSetState.Active -> AppColors.ActionTint
                AppSetState.Completed -> AppColors.Action
                AppSetState.Disabled -> AppColors.Disabled
            }
            val foreground = when (set.state) {
                AppSetState.Default -> AppColors.SecondaryText
                AppSetState.Active -> AppColors.Action
                AppSetState.Completed -> AppColors.Background
                AppSetState.Disabled -> AppColors.DisabledText
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
                enabled = enabled && (!completed || allowUndo) && !disabled,
                shape = CircleShape,
                color = background,
                contentColor = foreground,
                border = if (active) BorderStroke(AppDimensions.SetActiveStroke, AppColors.Action) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (completed) "✓" else (set.position + 1).toString(),
                        modifier = Modifier.clearAndSetSemantics {},
                        style = AppTypography.Label,
                    )
                }
            }
        }
    }
}

/** Active's smaller circles and Rest's larger circles share non-overlapping minimum 48dp targets. */
@Composable
private fun RefinedSetControl(
    set: AppSetControl,
    enabled: Boolean,
    resting: Boolean,
    allowUndo: Boolean,
    onComplete: (Int) -> Unit,
) {
    val completed = set.state == AppSetState.Completed
    val current = set.state == AppSetState.Active
    val disabled = set.state == AppSetState.Disabled || !enabled
    val labelStyle = if (resting) Rest.SetLabel else Active.SetLabel
    val visibleSize = maxOf(
        if (resting) Rest.SetDiameter else Active.SetDiameter,
        with(LocalDensity.current) { labelStyle.lineHeight.toDp() },
    )
    val targetSize = maxOf(AppDimensions.TouchTargetMin, visibleSize)
    Box(
        modifier = Modifier.size(targetSize)
            .clickable(
                enabled = (!completed || allowUndo) && !disabled,
                role = Role.Button,
                onClickLabel = if (completed && allowUndo) "Mark set incomplete" else "Complete set",
            ) { onComplete(set.position) }
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
