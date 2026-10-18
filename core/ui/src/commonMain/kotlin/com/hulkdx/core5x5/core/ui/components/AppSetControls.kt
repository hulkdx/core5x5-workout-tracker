package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.hulkdx.core5x5.core.ui.theme.AppActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppTypography

enum class AppSetState { Default, Active, Completed, Disabled }

data class AppSetControl(
    val position: Int,
    val state: AppSetState,
    val completionDescription: String,
    val reps: Int,
)

@Composable
fun AppSetControls(
    sets: List<AppSetControl>,
    onComplete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    refined: Boolean = false,
    enabled: Boolean = true,
    allowCompletedTap: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().testTag("set-row").horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        sets.forEach { set ->
            if (refined) {
                RefinedSetControl(set, enabled, allowCompletedTap, onComplete)
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
                modifier = Modifier.sizeIn(minWidth = size, minHeight = size).testTag("set:${set.position}").semantics {
                    contentDescription = set.completionDescription
                    stateDescription = when {
                        completed -> "Completed"
                        active -> "Next set"
                        disabled -> "Saving"
                        else -> "Incomplete"
                    }
                },
                enabled = enabled && (!completed || allowCompletedTap) && !disabled,
                shape = CircleShape,
                color = background,
                contentColor = foreground,
                border = if (active) BorderStroke(AppDimensions.SetActiveStroke, AppColors.Action) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = set.reps.toString(),
                        modifier = Modifier.clearAndSetSemantics {},
                        style = AppTypography.Label,
                    )
                }
            }
        }
    }
}

/** Set geometry stays stable through rest transitions, with non-overlapping minimum 48dp targets. */
@Composable
private fun RefinedSetControl(
    set: AppSetControl,
    enabled: Boolean,
    allowCompletedTap: Boolean,
    onComplete: (Int) -> Unit,
) {
    val completed = set.state == AppSetState.Completed
    val current = set.state == AppSetState.Active
    val disabled = set.state == AppSetState.Disabled || !enabled
    val labelStyle = Active.SetLabel
    val visibleSize = maxOf(
        Active.SetDiameter,
        with(LocalDensity.current) { labelStyle.lineHeight.toDp() },
    )
    val targetSize = maxOf(AppDimensions.TouchTargetMin, visibleSize)
    Box(
        modifier = Modifier.size(targetSize).testTag("set:${set.position}")
            .clickable(
                enabled = (!completed || allowCompletedTap) && !disabled,
                role = Role.Button,
                onClickLabel = when {
                    !completed -> "Complete set with ${set.reps} reps"
                    set.reps == 1 -> "Reset set to incomplete"
                    else -> "Record ${set.reps - 1} reps"
                },
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
                Text(
                    text = set.reps.toString(),
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
