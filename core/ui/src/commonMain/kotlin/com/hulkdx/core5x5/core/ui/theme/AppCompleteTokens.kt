package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** complete.* raster estimates from 06. CompleteWorkout.png, normalized to width 390. */
object AppCompleteTokens {
    val Background = Color(0xFF0A1013)
    val Card = Color(0xFF151B20)
    val Border = Color(0xFF303C43)
    val Action = Color(0xFF61DE8F)
    val ActionPressed = Color(0xFF4FCF76)
    val SystemTopReserve = 44.dp
    val SystemBottomReserve = 20.dp
    val Inset = 20.dp
    val BottomPadding = 58.dp
    val ContentGap = 18.dp
    val SuccessSize = 64.dp
    val SuccessTitleGap = 14.dp
    val SubtitleGap = 6.dp
    val MetricsTopGap = 32.dp
    val MetricsGap = 10.dp
    val MetricHeight = 78.dp
    val CardPadding = 14.dp
    val MetricPaddingVertical = 12.dp
    val MetricLabelGap = 4.dp
    val NextHeight = 84.dp
    val NextLabelGap = 6.dp
    val BadgeSize = 38.dp
    val BadgeRadius = 8.dp
    val CardRadius = 12.dp
    val BorderStroke = 1.dp
    val ButtonHeight = 52.dp
    val Title = AppTypography.Title.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val Subtitle = AppTypography.Body.copy(fontSize = 14.sp, lineHeight = 22.sp)
    val Label = AppTypography.Caption
    val MetricValue = AppTypography.Title.copy(fontWeight = FontWeight.Bold)
    val NextTitle = AppTypography.Heading.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val BadgeLabel = AppTypography.Heading.copy(fontWeight = FontWeight.Bold)
    val ButtonLabel = AppTypography.Heading.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
}
