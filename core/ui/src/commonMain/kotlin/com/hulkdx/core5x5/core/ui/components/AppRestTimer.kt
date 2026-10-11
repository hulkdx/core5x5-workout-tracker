package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTypography
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppRestTokens as Rest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRestTimer(
    countdown: String,
    isExpired: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val label = if (isExpired) "Rest complete" else "Rest"
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(
            min = if (compact) AppDimensions.TimerPinnedMinHeight else Rest.TimerHeight,
        ).semantics(mergeDescendants = true) {
            contentDescription = "$label, $countdown"
            // Foreground expiry is announced once; ticking time is available on demand.
            if (isExpired) liveRegion = LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(AppHomeTokens.CardRadius),
        color = AppHomeTokens.Card,
        border = BorderStroke(AppHomeTokens.BorderStroke, AppHomeTokens.Border),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) AppDimensions.TimerPadding else Rest.TimerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Rest.TimerLabelGap, Alignment.CenterVertically),
        ) {
            if (compact) FlowRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.TimerTopHeight),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.TimerLabelGap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.TimerLabelGap, Alignment.CenterVertically),
            ) {
                Text(
                    text = if (isExpired) label else "REST",
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics {},
                    style = AppTypography.Caption,
                    color = if (isExpired) AppHomeTokens.Action else AppHomeTokens.Secondary,
                )
                Text(
                    text = countdown,
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics {},
                    style = AppTypography.Numeric,
                    color = if (isExpired) AppHomeTokens.Action else AppHomeTokens.Primary,
                )
            } else {
                Text(
                    text = if (isExpired) label else "REST",
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    style = Rest.TimerLabel,
                    textAlign = TextAlign.Center,
                    color = if (isExpired) AppHomeTokens.Action else AppHomeTokens.Secondary,
                )
                Text(
                    text = countdown,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    style = Rest.Countdown,
                    textAlign = TextAlign.Center,
                    color = if (isExpired) AppHomeTokens.Action else AppHomeTokens.Primary,
                )
            }
        }
    }
}
