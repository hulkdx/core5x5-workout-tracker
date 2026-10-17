package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** history.* geometry from 07 History.png; colors align with Today by user direction. */
object AppHistoryTokens {
    val Background = AppHomeTokens.Background
    val Card = AppHomeTokens.Card
    val DateTile = Color(0xFF1D2022)
    val Border = AppHomeTokens.Border
    val Primary = AppHomeTokens.Primary
    val Secondary = AppHomeTokens.Secondary
    val Action = AppHomeTokens.Action
    val Inset = 20.dp
    val TopGap = 14.dp
    val TitleGap = 16.dp
    val MonthGap = 18.dp
    val MonthCardGap = 6.dp
    val CardGap = 6.dp
    val BottomPadding = 24.dp
    val CardHeight = 76.dp
    val CardPadding = 8.dp
    val CardRadius = 9.dp
    val BorderStroke = 1.dp
    val DateWidth = 56.dp
    val DateHeight = 58.dp
    val DateRadius = 10.dp
    val DateGap = 2.dp
    val DateContentGap = 14.dp
    val TextGap = 2.dp
    val TitleSummaryGap = 4.dp
    val ClockSize = 14.dp
    val ClockGap = 6.dp
    val GraphPadding = 16.dp
    val GraphGap = 12.dp
    val GraphHeight = 160.dp
    val GraphStroke = 2.dp
    val GraphPointRadius = 3.dp
    val Title = AppTypography.Title.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
    val Month = AppTypography.Caption.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    val Workout = AppTypography.Label.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val Metadata = AppTypography.Caption.copy(fontSize = 11.sp, lineHeight = 16.sp)
    val Weekday = Metadata
    val Day = AppTypography.Heading.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
}
