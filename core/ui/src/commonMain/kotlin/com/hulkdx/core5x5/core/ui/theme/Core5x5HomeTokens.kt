package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** home.* raster estimates from 01. HomeScreen.png; see design/specs/today.md. */
object Core5x5HomeTokens {
    val Background = Color(0xFF091013)
    val Card = Color(0xFF0C1317)
    val Primary = Color(0xFFF5F8F7)
    val Secondary = Color(0xFFA7B5C9)
    val Action = Color(0xFF35E575)
    val ActionPressed = Color(0xFF2FCC67)
    val Border = Color(0xFF1C2931)
    val SystemTopReserve = 44.dp
    val SystemBottomReserve = 20.dp
    val Inset = 16.dp
    val BrandInset = 20.dp
    val TopGap = 12.dp
    val BrandGap = 16.dp
    val CardPadding = 14.dp
    val CardRadius = 10.dp
    val TextInset = 6.dp
    val TextGap = 2.dp
    val HeadingGap = 16.dp
    val RowHeight = 72.dp
    val RowPadding = 12.dp
    val IconSize = 20.dp
    val ButtonGap = 12.dp
    val ButtonHeight = 52.dp
    val ControlRadius = 8.dp
    val SwitchPadding = 10.dp
    val SwitchHeight = 34.dp
    val SwitchGap = 8.dp
    val BorderStroke = 1.dp
    val NavigationHeight = 70.dp
    val NavigationPadding = 10.dp
    val NavigationPaddingHorizontal = 24.dp
    val NavigationItemHeight = 50.dp
    val NavigationIconSize = 28.dp
    val NavigationLabelGap = 4.dp
    val Brand = Core5x5Typography.Title.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val Title = Core5x5Typography.Title.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val Eyebrow = Core5x5Typography.Caption.copy(fontWeight = FontWeight.SemiBold)
    val Exercise = Core5x5Typography.Heading.copy(fontSize = 18.sp, lineHeight = 24.sp)
    val Prescription = Core5x5Typography.Label.copy(lineHeight = 22.sp)
    val Button = Core5x5Typography.Heading.copy(fontWeight = FontWeight.Bold)
    val SwitchLabel = Core5x5Typography.Caption.copy(fontWeight = FontWeight.SemiBold)
}
