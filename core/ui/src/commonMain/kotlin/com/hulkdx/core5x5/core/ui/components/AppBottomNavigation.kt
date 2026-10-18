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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppTypography

private val TopLevelTab.label: String
    get() = when (this) {
        TopLevelTab.TODAY -> "Today"
        TopLevelTab.HISTORY -> "History"
        TopLevelTab.SETTINGS -> "Settings"
    }

/** TopLevelScreenLayout owns system insets; this component owns only internal padding. */
@Composable
fun AppBottomNavigation(
    selectedItem: TopLevelTab,
    onItemSelected: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
    useHomeDesign: Boolean = true,
) {
    val labelStyle = if (useHomeDesign) Home.Eyebrow else AppTypography.Caption
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) {
        val widestLabel = TopLevelTab.entries.maxOf { item ->
            textMeasurer.measure(AnnotatedString(item.label), labelStyle, softWrap = false).size.width
        }
        // Leave one pixel for rounding when side padding divides the available space.
        (widestLabel + 1).toDp()
    }
    val preferredItemWidth = maxOf(AppDimensions.NavigationItemWidth, labelWidth)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(if (useHomeDesign) Home.Card else AppColors.Elevated)
            .drawBehind {
                if (useHomeDesign) drawLine(Home.Border, Offset.Zero, Offset(size.width, 0f), Home.BorderStroke.toPx())
            }
            .heightIn(min = if (useHomeDesign) Home.NavigationHeight else AppDimensions.NavigationHeight),
    ) {
        val horizontalPadding = minOf(
            if (useHomeDesign) Home.NavigationPaddingHorizontal else AppDimensions.NavigationPaddingHorizontal,
            ((maxWidth - preferredItemWidth * TopLevelTab.entries.size) / 2).coerceAtLeast(0.dp),
        )
        val itemWidth = minOf(
            preferredItemWidth,
            (maxWidth - horizontalPadding * 2) / TopLevelTab.entries.size,
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = if (useHomeDesign) Home.NavigationPadding else AppDimensions.NavigationPaddingVertical,
                )
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TopLevelTab.entries.forEach { item ->
                val selected = item == selectedItem
                val color = if (useHomeDesign) {
                    if (selected) Home.Action else Home.Secondary
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
                        modifier = Modifier.size(if (useHomeDesign) Home.NavigationIconSize else AppDimensions.NavigationIconSize),
                        tint = color,
                    )
                    Text(
                        text = item.label,
                        style = labelStyle,
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
