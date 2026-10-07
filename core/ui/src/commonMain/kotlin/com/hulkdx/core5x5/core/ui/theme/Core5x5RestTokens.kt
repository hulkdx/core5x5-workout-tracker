package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** rest.* raster estimates from 03. ActiveWorkout - Rest Screen.png at width 390. */
object Core5x5RestTokens {
    val CardPaddingVertical = 18.dp
    val SetGap = 16.dp
    val SetDiameter = 48.dp
    val Exercise = Core5x5ActiveTokens.Exercise.copy(fontSize = 24.sp, lineHeight = 30.sp)
    val Metadata = Core5x5ActiveTokens.Metadata.copy(fontSize = 17.sp, lineHeight = 24.sp)
    val SetLabel = Core5x5ActiveTokens.SetLabel.copy(fontSize = 20.sp, lineHeight = 26.sp)
    val TimerHeight = 122.dp
    val TimerPadding = 16.dp
    val TimerLabelGap = 4.dp
    val TimerLabel = Core5x5Typography.Caption.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
    val Countdown = Core5x5Typography.Numeric.copy(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold)
    val FinishLabel = Core5x5Typography.Body.copy(lineHeight = 22.sp)
}
