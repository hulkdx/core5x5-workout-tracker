package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** settings.* geometry from 08. Settings screen.png; colors align with Today by user direction. */
object AppSettingsTokens {
    val Background = AppHomeTokens.Background
    val Card = AppHomeTokens.Card
    val Primary = AppHomeTokens.Primary
    val Secondary = AppHomeTokens.Secondary
    val Border = AppHomeTokens.Border
    val Action = AppHomeTokens.Action
    val Inset = 18.dp
    val TopGap = 12.dp
    val TitleGap = 22.dp
    val SectionGap = 22.dp
    val HeadingGap = 6.dp
    val CardInset = 16.dp
    val CardRadius = 8.dp
    val RowHeight = 48.dp // 45dp raster row enlarged to the minimum touch target
    val RowPaddingVertical = 12.dp
    val ValueGap = 12.dp
    val ChevronSize = 12.dp
    val BorderStroke = 1.dp
    val BottomPadding = 18.dp
    val Title = AppTypography.Title.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val Section = AppTypography.Caption.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val Label = AppTypography.Body.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val Value = AppTypography.Caption.copy(fontSize = 13.sp, lineHeight = 20.sp)
}
