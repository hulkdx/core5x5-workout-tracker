package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

/** A full-card target for a saved workout history entry. */
@Composable
fun Core5x5HistoryCard(
    badge: String,
    title: String,
    dateAndDuration: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusLarge),
        color = Core5x5Colors.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(Core5x5Dimensions.HistoryPadding),
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.HistoryGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.HistoryHeadingMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistoryBadge(badge)
                Column(
                    modifier = Modifier.weight(1f).padding(start = Core5x5Dimensions.HistoryBadgeGap),
                ) {
                    Text(text = title, style = Core5x5Typography.Label, color = Core5x5Colors.PrimaryText)
                    Text(text = dateAndDuration, style = Core5x5Typography.Caption, color = Core5x5Colors.SecondaryText)
                }
                Text(
                    text = "›",
                    modifier = Modifier.clearAndSetSemantics {},
                    style = Core5x5Typography.Heading,
                    color = Core5x5Colors.MutedText,
                )
            }
            Text(
                text = summary,
                modifier = Modifier.fillMaxWidth(),
                style = Core5x5Typography.Caption,
                color = Core5x5Colors.SecondaryText,
            )
        }
    }
}

@Composable
private fun HistoryBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusSmall),
        color = Core5x5Colors.Subtle,
        border = androidx.compose.foundation.BorderStroke(
            width = Core5x5Dimensions.BorderStroke,
            color = Core5x5Colors.Border,
        ),
    ) {
        Box(
            modifier = Modifier.sizeIn(
                minWidth = Core5x5Dimensions.IndicatorSize,
                minHeight = Core5x5Dimensions.IndicatorSize,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = Core5x5Typography.Label, color = Core5x5Colors.SecondaryText)
        }
    }
}
