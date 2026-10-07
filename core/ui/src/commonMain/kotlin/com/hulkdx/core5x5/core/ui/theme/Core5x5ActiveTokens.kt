package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** active.* estimates from 02. ActiveWorkout Screen.png; shared styling comes from Today. */
object Core5x5ActiveTokens {
    val Inset = 18.dp
    val TopGap = 8.dp
    val CardPaddingHorizontal = 18.dp
    val CardPaddingVertical = Core5x5HomeTokens.CardPadding
    val SetGap = 6.dp
    val SetDiameter = 42.dp
    val SetActiveStroke = 2.dp
    val TopBarHeight = Core5x5Dimensions.TouchTargetMin
    val SetSurface = Color(0xFF1D2022)
    val Exercise = Core5x5Typography.Heading.copy(fontSize = 22.sp, lineHeight = 28.sp)
    val Metadata = Core5x5Typography.Body.copy(lineHeight = 22.sp)
    val SetLabel = Core5x5HomeTokens.Exercise
}
