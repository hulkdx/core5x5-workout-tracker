package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** settings.* raster estimates from 08. Settings screen.png; see design/specs/settings.md. */
object Core5x5SettingsTokens {
    val Background = Color(0xFF091115)
    val Card = Color(0xFF10191E)
    val Primary = Color(0xFFF5F8F7)
    val Secondary = Color(0xFFA7B5C9)
    val Border = Color(0xFF28353D)
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
    val Title = Core5x5Typography.Title.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val Section = Core5x5Typography.Caption.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val Label = Core5x5Typography.Body.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val Value = Core5x5Typography.Caption.copy(fontSize = 13.sp, lineHeight = 20.sp)
}
