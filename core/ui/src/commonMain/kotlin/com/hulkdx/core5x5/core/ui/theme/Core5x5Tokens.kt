package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Measured dark tokens from the workspace's design/tokens.json. */
object Core5x5Colors {
    val Background = Color(0xFF0C1114)
    val Elevated = Color(0xFF151B20)
    val Subtle = Color(0xFF11171B)
    val Pressed = Color(0xFF1C252A)
    val Disabled = Color(0xFF1D252A)
    val PrimaryText = Color(0xFFF5F8F7)
    val SecondaryText = Color(0xFF9BA6AD)
    val MutedText = Color(0xFF68747A)
    val DisabledText = Color(0xFF59656C)
    val Action = Color(0xFF67E38B)
    val ActionPressed = Color(0xFF4FCF76)
    val Border = Color(0xFF273137)
    val Destructive = Color(0xFFF06C6C)
}

/** The type.* measurements, using platform sans-serif in place of the reference Inter font. */
object Core5x5Typography {
    val Title = style(size = 24, lineHeight = 30, weight = FontWeight.SemiBold)
    val Heading = style(size = 20, lineHeight = 26, weight = FontWeight.SemiBold)
    val Body = style(size = 16, lineHeight = 23, weight = FontWeight.Normal)
    val Label = style(size = 15, lineHeight = 20, weight = FontWeight.Medium)
    val Caption = style(size = 13, lineHeight = 18, weight = FontWeight.Normal)

    private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = 0.sp,
    )
}

object Core5x5Dimensions {
    val ReferenceSystemTopReserve = 38.dp // reference.systemTopReserve; reference previews only
    val ScreenInset = 20.dp // layout.screenInset
    val TextGap = 2.dp // spacing.s2
    val ContentGap = 14.dp // layout.contentGap
    val ContentPaddingVertical = 18.dp // layout.contentPaddingVertical
    val RadiusSmall = 8.dp // radius.small
    val RadiusMedium = 12.dp // radius.medium
    val RadiusLarge = 16.dp // radius.large
    val BorderStroke = 1.dp // component.border.stroke
    val ButtonHeight = 56.dp // component.button.height
    val ButtonPadding = 0.dp // component.button.contentPadding
    val IndicatorSize = 34.dp // component.indicator.size
    val CardPadding = 14.dp // component.workoutCard.padding
    val CardGap = 10.dp // component.workoutCard.gap
    val CardHeadingMinHeight = 44.dp // component.workoutCard.headingMinHeight
    val ExerciseRowHeight = 72.dp // component.exerciseRow.height
    val ExerciseRowInset = 32.dp // component.exerciseRow.contentInset replaces horizontal padding
    val ExerciseRowPaddingVertical = 14.dp // component.exerciseRow.padding
    val ExerciseRowGap = 12.dp // component.exerciseRow.gap
    val ExerciseStatusSize = 44.dp // component.exerciseRow.statusSize
    val MetricsRowHeight = 86.dp // component.metrics.rowHeight
    val MetricsGap = 10.dp // component.metrics.gap
    val MetricPaddingHorizontal = 14.dp // component.metrics.paddingHorizontal
    val MetricPaddingVertical = 12.dp // component.metrics.paddingVertical
    val MetricLabelGap = 10.dp // component.metrics.labelGap
    val SuccessSize = 64.dp // component.success.size
    val CompleteGap = 18.dp // layout.completeGap
    val CompleteExtraTop = 16.dp // layout.completeExtraTop, after real safe insets
    val CompletePaddingBottom = 24.dp // layout.completePaddingBottom
}
