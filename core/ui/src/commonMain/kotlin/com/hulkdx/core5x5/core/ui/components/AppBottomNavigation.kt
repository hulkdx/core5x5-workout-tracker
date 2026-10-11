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
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHistoryTokens as History
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppTypography

enum class AppNavigationItem(val label: String) {
    TODAY("Today"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

/** The app shell owns system insets; this component owns only the design's internal padding. */
@Composable
fun AppBottomNavigation(
    selectedItem: AppNavigationItem,
    onItemSelected: (AppNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    useHomeDesign: Boolean = selectedItem == AppNavigationItem.TODAY || selectedItem == AppNavigationItem.HISTORY,
) {
    val isHistoryDesign = useHomeDesign && selectedItem == AppNavigationItem.HISTORY
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHistoryDesign) History.Card else if (useHomeDesign) Home.Card else AppColors.Elevated)
            .drawBehind {
                if (useHomeDesign) drawLine(if (isHistoryDesign) History.Border else Home.Border, Offset.Zero, Offset(size.width, 0f), Home.BorderStroke.toPx())
            }
            .heightIn(min = if (useHomeDesign) Home.NavigationHeight else AppDimensions.NavigationHeight)
            .padding(
                horizontal = if (useHomeDesign) Home.NavigationPaddingHorizontal else AppDimensions.NavigationPaddingHorizontal,
                vertical = if (useHomeDesign) Home.NavigationPadding else AppDimensions.NavigationPaddingVertical,
            ),
    ) {
        val itemWidth = minOf(
            AppDimensions.NavigationItemWidth,
            maxWidth / AppNavigationItem.entries.size,
        )
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppNavigationItem.entries.forEach { item ->
                val selected = item == selectedItem
                val color = if (useHomeDesign) {
                    if (selected) Home.Action else if (isHistoryDesign) History.Secondary else Home.Secondary
                } else {
                    if (selected) AppColors.Action else AppColors.MutedText
                }
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .heightIn(min = if (useHomeDesign) Home.NavigationItemHeight else AppDimensions.NavigationItemHeight)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onItemSelected(item) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        if (useHomeDesign) Home.NavigationLabelGap else AppDimensions.NavigationLabelGap,
                        Alignment.CenterVertically,
                    ),
                ) {
                    Icon(
                        imageVector = if (useHomeDesign) item.homeIcon else item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (isHistoryDesign) History.NavigationIconSize else if (useHomeDesign) Home.NavigationIconSize else AppDimensions.NavigationIconSize),
                        tint = color,
                    )
                    Text(
                        text = item.label,
                        style = if (isHistoryDesign) History.Metadata else if (useHomeDesign) Home.Eyebrow else AppTypography.Caption,
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
