package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home

enum class Core5x5NavigationItem(val label: String) {
    TODAY("Today"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

/** The app shell owns system insets; this component owns only the design's internal padding. */
@Composable
fun Core5x5BottomNavigation(
    selectedItem: Core5x5NavigationItem,
    onItemSelected: (Core5x5NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    useHomeDesign: Boolean = selectedItem == Core5x5NavigationItem.TODAY,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(if (useHomeDesign) Home.Card else Core5x5Colors.Elevated)
            .drawBehind {
                if (useHomeDesign) drawLine(Home.Border, Offset.Zero, Offset(size.width, 0f), Home.BorderStroke.toPx())
            }
            .heightIn(min = if (useHomeDesign) Home.NavigationHeight else Core5x5Dimensions.NavigationHeight)
            .padding(
                horizontal = if (useHomeDesign) Home.NavigationPaddingHorizontal else Core5x5Dimensions.NavigationPaddingHorizontal,
                vertical = if (useHomeDesign) Home.NavigationPadding else Core5x5Dimensions.NavigationPaddingVertical,
            ),
    ) {
        val itemWidth = minOf(
            Core5x5Dimensions.NavigationItemWidth,
            maxWidth / Core5x5NavigationItem.entries.size,
        )
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Core5x5NavigationItem.entries.forEach { item ->
                val selected = item == selectedItem
                val color = if (useHomeDesign) {
                    if (selected) Home.Action else Home.Secondary
                } else {
                    if (selected) Core5x5Colors.Action else Core5x5Colors.MutedText
                }
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .heightIn(min = if (useHomeDesign) Home.NavigationItemHeight else Core5x5Dimensions.NavigationItemHeight)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onItemSelected(item) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        if (useHomeDesign) Home.NavigationLabelGap else Core5x5Dimensions.NavigationLabelGap,
                        Alignment.CenterVertically,
                    ),
                ) {
                    Icon(
                        imageVector = if (useHomeDesign) item.homeIcon else item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (useHomeDesign) Home.NavigationIconSize else Core5x5Dimensions.NavigationIconSize),
                        tint = color,
                    )
                    Text(
                        text = item.label,
                        style = if (useHomeDesign) Home.Eyebrow else Core5x5Typography.Caption,
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
