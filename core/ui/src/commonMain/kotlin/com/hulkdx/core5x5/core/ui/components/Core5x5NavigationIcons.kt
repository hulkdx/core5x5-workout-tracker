package com.hulkdx.core5x5.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal val Core5x5NavigationItem.icon: ImageVector
    get() = when (this) {
        Core5x5NavigationItem.TODAY -> HomeIcon
        Core5x5NavigationItem.HISTORY -> HistoryIcon
        Core5x5NavigationItem.SETTINGS -> SettingsIcon
    }

/** Native vectors matching the revised Home PNG; the original SVG family remains below. */
internal val Core5x5NavigationItem.homeIcon: ImageVector
    get() = when (this) {
        Core5x5NavigationItem.TODAY -> FilledHomeIcon
        Core5x5NavigationItem.HISTORY -> ClockHistoryIcon
        Core5x5NavigationItem.SETTINGS -> FilledSettingsIcon
    }

private val FilledHomeIcon = ImageVector.Builder(
    name = "Filled home", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
    addPath(pathData = addPathNodes("M2 10Q2 9 3 8L11 2Q12 1 13 2L21 8Q22 9 22 10V21Q22 22 21 22H15V15H9V22H3Q2 22 2 21Z"), fill = SolidColor(Color.Black))
}.build()

private val ClockHistoryIcon = navigationIcon(
    name = "Clock history",
    path = "M2 12a10 10 0 1 1 3 7M12 5v7l5 3",
)

private val FilledSettingsIcon = ImageVector.Builder(
    name = "Filled settings", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
    addPath(
        pathData = addPathNodes(
            "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z" +
                "M19 12a7 7 0 0 0-.12-1.27l2.02-1.58-2-3.46-2.49 1a7.1 7.1 0 0 0-2.2-1.27" +
                "L13.88 3h-4l-.33 2.42a7.1 7.1 0 0 0-2.2 1.27l-2.49-1-2 3.46 2.02 1.58" +
                "A7 7 0 0 0 4.76 12c0 .43.04.85.12 1.27l-2.02 1.58 2 3.46 2.49-1" +
                "a7.1 7.1 0 0 0 2.2 1.27L9.88 21h4l.33-2.42a7.1 7.1 0 0 0 2.2-1.27" +
                "l2.49 1 2-3.46-2.02-1.58c.08-.42.12-.84.12-1.27Z",
        ),
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd,
    )
}.build()

// Exact SVG paths from design/assets/icons, retaining the 24 × 24 viewBox whitespace.
// Icon's tint supplies the source currentColor on both Android and iOS.
private val HomeIcon = navigationIcon(
    name = "Home",
    path = "M3 10.8 12 3l9 7.8M5.5 9.8V21h5.2v-6.2h2.6V21h5.2V9.8",
)

private val HistoryIcon = navigationIcon(
    name = "History",
    path = "M4 20V11M10 20V4M16 20v-7M22 20V8",
)

private val SettingsIcon = navigationIcon(
    name = "Settings",
    path = "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z" +
        "M19 12a7 7 0 0 0-.12-1.27l2.02-1.58-2-3.46-2.49 1a7.1 7.1 0 0 0-2.2-1.27" +
        "L13.88 3h-4l-.33 2.42a7.1 7.1 0 0 0-2.2 1.27l-2.49-1-2 3.46 2.02 1.58" +
        "A7 7 0 0 0 4.76 12c0 .43.04.85.12 1.27l-2.02 1.58 2 3.46 2.49-1" +
        "a7.1 7.1 0 0 0 2.2 1.27L9.88 21h4l.33-2.42a7.1 7.1 0 0 0 2.2-1.27" +
        "l2.49 1 2-3.46-2.02-1.58c.08-.42.12-.84.12-1.27Z",
)

private fun navigationIcon(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = addPathNodes(path),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()
