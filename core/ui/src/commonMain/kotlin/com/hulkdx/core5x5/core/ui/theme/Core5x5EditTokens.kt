package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Raster estimates normalized from the two 838px edit references to a 390dp viewport. */
object Core5x5EditTokens {
    val Inset = 18.dp
    val Gap = 12.dp
    val CardPadding = 14.dp
    val NameGap = 8.dp
    val Control = Color(0xFF232A30)
    val Sheet = Color(0xFF171E23)
    val FieldBorder = Color(0xFF39454E)
    val Title = Core5x5HomeTokens.Title.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val Body = Core5x5HomeTokens.Prescription.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val Value = Body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Picker = Value.copy(fontSize = 20.sp, lineHeight = 26.sp)
}
