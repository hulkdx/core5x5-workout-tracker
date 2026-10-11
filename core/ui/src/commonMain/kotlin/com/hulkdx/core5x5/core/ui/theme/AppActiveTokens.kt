package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** active.* estimates from 02. ActiveWorkout Screen.png; shared styling comes from Today. */
object AppActiveTokens {
    val Inset = 18.dp
    val TopGap = 8.dp
    val CardPaddingHorizontal = 18.dp
    val CardPaddingVertical = AppHomeTokens.CardPadding
    val SetGap = 6.dp
    val SetDiameter = 42.dp
    val SetActiveStroke = 2.dp
    val TopBarHeight = AppDimensions.TouchTargetMin
    val SetSurface = Color(0xFF1D2022)
    val Exercise = AppTypography.Heading.copy(fontSize = 22.sp, lineHeight = 28.sp)
    val Metadata = AppTypography.Body.copy(lineHeight = 22.sp)
    val SetLabel = AppHomeTokens.Exercise
}
