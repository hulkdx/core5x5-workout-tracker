package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

enum class Core5x5Tab(val label: String) { TODAY("Today"), HISTORY("History"), SETTINGS("Settings") }

@Composable
fun Core5x5BottomNavigation(
    selectedTab: Core5x5Tab,
    onSelectTab: (Core5x5Tab) -> Unit,
    modifier: Modifier = Modifier,
    historyEnabled: Boolean = false,
    bottomInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().background(Core5x5Colors.Elevated)
            .windowInsetsPadding(bottomInsets)
            .padding(
                horizontal = Core5x5Dimensions.NavigationPaddingHorizontal,
                vertical = Core5x5Dimensions.NavigationPaddingVertical,
            ),
    ) {
        val itemWidth = minOf(Core5x5Dimensions.NavigationItemWidth, maxWidth / Core5x5Tab.entries.size)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Core5x5Tab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val tint = if (selected) Core5x5Colors.Action else Core5x5Colors.MutedText
                Column(
                    modifier = Modifier.width(itemWidth)
                        .heightIn(min = Core5x5Dimensions.NavigationItemHeight)
                        .selectable(
                            selected = selected,
                            enabled = tab != Core5x5Tab.HISTORY || historyEnabled,
                            role = Role.Tab,
                            onClick = { onSelectTab(tab) },
                        ),
                    verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.NavigationLabelGap, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = navigationIcons.getValue(tab),
                        contentDescription = null,
                        modifier = Modifier.size(Core5x5Dimensions.NavigationIconSize),
                        tint = tint,
                    )
                    Text(text = tab.label, style = Core5x5Typography.Caption, color = tint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// Paths, 24×24 viewBox, and stroke geometry copied from design/assets/icons/*.svg.
// White is a vector placeholder; Icon applies the semantic currentColor tint.
private val navigationIcons = mapOf(
    Core5x5Tab.TODAY to navigationIcon("home", "M3 10.8 12 3l9 7.8M5.5 9.8V21h5.2v-6.2h2.6V21h5.2V9.8"),
    Core5x5Tab.HISTORY to navigationIcon("history", "M4 20V11M10 20V4M16 20v-7M22 20V8"),
    Core5x5Tab.SETTINGS to navigationIcon(
        "settings",
        "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4ZM19 12a7 7 0 0 0-.12-1.27l2.02-1.58-2-3.46-2.49 1a7.1 7.1 0 0 0-2.2-1.27L13.88 3h-4l-.33 2.42a7.1 7.1 0 0 0-2.2 1.27l-2.49-1-2 3.46 2.02 1.58A7 7 0 0 0 4.76 12c0 .43.04.85.12 1.27l-2.02 1.58 2 3.46 2.49-1a7.1 7.1 0 0 0 2.2 1.27L9.88 21h4l.33-2.42a7.1 7.1 0 0 0 2.2-1.27l2.49 1 2-3.46-2.02-1.58c.08-.42.12-.84.12-1.27Z",
    ),
)

private fun navigationIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
