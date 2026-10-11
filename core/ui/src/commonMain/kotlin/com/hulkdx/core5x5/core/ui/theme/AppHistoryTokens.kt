package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** history.* raster estimates from 07 History.png, normalized to width 390. */
object AppHistoryTokens {
    val Background = Color(0xFF0B1216)
    val Card = Color(0xFF10181D)
    val DateTile = Color(0xFF192229)
    val Border = Color(0xFF202A30)
    val Primary = Color(0xFFF5F7F8)
    val Secondary = Color(0xFFA8B3C0)
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
    val NavigationIconSize = 24.dp
    val Title = AppTypography.Title.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
    val Month = AppTypography.Caption.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    val Workout = AppTypography.Label.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val Metadata = AppTypography.Caption.copy(fontSize = 11.sp, lineHeight = 16.sp)
    val Weekday = Metadata
    val Day = AppTypography.Heading.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
}
