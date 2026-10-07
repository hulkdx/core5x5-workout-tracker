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
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens
import com.hulkdx.core5x5.core.ui.theme.Core5x5RestTokens as Rest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Core5x5RestTimer(
    countdown: String,
    isExpired: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val label = if (isExpired) "Rest complete" else "Rest"
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(
            min = if (compact) Core5x5Dimensions.TimerPinnedMinHeight else Rest.TimerHeight,
        ).semantics(mergeDescendants = true) {
            contentDescription = "$label, $countdown"
            // Foreground expiry is announced once; ticking time is available on demand.
            if (isExpired) liveRegion = LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(Core5x5HomeTokens.CardRadius),
        color = Core5x5HomeTokens.Card,
        border = BorderStroke(Core5x5HomeTokens.BorderStroke, Core5x5HomeTokens.Border),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) Core5x5Dimensions.TimerPadding else Rest.TimerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Rest.TimerLabelGap, Alignment.CenterVertically),
        ) {
            if (compact) FlowRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.TimerTopHeight),
                horizontalArrangement = Arrangement.spacedBy(Core5x5Dimensions.TimerLabelGap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.TimerLabelGap, Alignment.CenterVertically),
            ) {
                Text(
                    text = if (isExpired) label else "REST",
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics {},
                    style = Core5x5Typography.Caption,
                    color = if (isExpired) Core5x5HomeTokens.Action else Core5x5HomeTokens.Secondary,
                )
                Text(
                    text = countdown,
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics {},
                    style = Core5x5Typography.Numeric,
                    color = if (isExpired) Core5x5HomeTokens.Action else Core5x5HomeTokens.Primary,
                )
            } else {
                Text(
                    text = if (isExpired) label else "REST",
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    style = Rest.TimerLabel,
                    textAlign = TextAlign.Center,
                    color = if (isExpired) Core5x5HomeTokens.Action else Core5x5HomeTokens.Secondary,
                )
                Text(
                    text = countdown,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    style = Rest.Countdown,
                    textAlign = TextAlign.Center,
                    color = if (isExpired) Core5x5HomeTokens.Action else Core5x5HomeTokens.Primary,
                )
            }
        }
    }
}
