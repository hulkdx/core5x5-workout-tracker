package com.hulkdx.core5x5.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Implementation motion correction documented in design/DESIGN_SYSTEM.md. */
object AppMotion {
    const val NavigationDurationMillis = 200
}

/** Shared semantic colors; dark values retain the established application palette. */
object AppColors {
    val Background: Color @Composable get() = themeColor(Color(0xFF0C1114), Color(0xFFF4F6F5))
    val Elevated: Color @Composable get() = themeColor(Color(0xFF151B20), Color(0xFFFFFFFF))
    val Subtle: Color @Composable get() = themeColor(Color(0xFF11171B), Color(0xFFEDF1EF))
    val Pressed: Color @Composable get() = themeColor(Color(0xFF1C252A), Color(0xFFE1E8E4))
    val Disabled: Color @Composable get() = themeColor(Color(0xFF1D252A), Color(0xFFE4E9E6))
    val PrimaryText: Color @Composable get() = themeColor(Color(0xFFF5F8F7), Color(0xFF18211C))
    val SecondaryText: Color @Composable get() = themeColor(Color(0xFF9BA6AD), Color(0xFF526159))
    val MutedText: Color @Composable get() = themeColor(Color(0xFF68747A), Color(0xFF59675E))
    val DisabledText: Color @Composable get() = themeColor(Color(0xFF59656C), Color(0xFF748178))
    val OnAction: Color @Composable get() = themeColor(Color(0xFF0E1011), Color.White)
    // Shared green accents for every screen, including completion and legacy controls.
    val Action: Color @Composable get() = themeColor(Color(0xFF35E575), Color(0xFF147638))
    val ActionPressed: Color @Composable get() = themeColor(Color(0xFF2FCC67), Color(0xFF106331))
    val ActionTint: Color @Composable get() = themeColor(Color(0xFF183823), Color(0xFFDCEFE2))
    val Border: Color @Composable get() = themeColor(Color(0xFF273137), Color(0xFFCDD7D0))
    val Destructive: Color @Composable get() = themeColor(Color(0xFFF06C6C), Color(0xFFBA3030))
}

/** The type.* measurements, using platform sans-serif in place of the reference Inter font. */
object AppTypography {
    val Title = style(size = 24, lineHeight = 30, weight = FontWeight.SemiBold)
    val Heading = style(size = 20, lineHeight = 26, weight = FontWeight.SemiBold)
    val Body = style(size = 16, lineHeight = 23, weight = FontWeight.Normal)
    val Label = style(size = 15, lineHeight = 20, weight = FontWeight.Medium)
    val Caption = style(size = 13, lineHeight = 18, weight = FontWeight.Normal)
    val Numeric = style(size = 28, lineHeight = 32, weight = FontWeight.SemiBold)

    private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = 0.sp,
    )
}

object AppDimensions {
    val ReferenceWidth = 390.dp // reference.width
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
    val TopBarHeight = 44.dp // component.topBar.visualHeight
    val SetDiameter = 48.dp // component.set.diameter
    val SetActiveStroke = 1.5.dp // component.set.activeStroke
    val TimerPinnedMinHeight = 66.dp // component.timer.pinnedMinHeight: 14 + 38 + 14
    val TimerPadding = 14.dp // component.timer.padding
    val TimerTopHeight = 38.dp // component.timer.topHeight
    val TimerLabelGap = 10.dp // component.timer.labelGap
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
    val RestGap = 12.dp // layout.restGap
    val TouchTargetMin = 48.dp // accessibility.touchTargetMin
    val SettingHeight = 58.dp // component.setting.height
    val NavigationHeight = 74.dp // component.navigation.height
    val NavigationPaddingHorizontal = 28.dp // component.navigation.paddingHorizontal
    val NavigationPaddingVertical = 8.dp // component.navigation.paddingVertical
    val NavigationItemWidth = 86.dp // component.navigation.itemWidth
    val NavigationItemHeight = 58.dp // component.navigation.itemHeight
    val NavigationIconSize = 22.dp // component.navigation.iconSize
    val NavigationLabelGap = 2.dp // component.navigation.labelGap
}
