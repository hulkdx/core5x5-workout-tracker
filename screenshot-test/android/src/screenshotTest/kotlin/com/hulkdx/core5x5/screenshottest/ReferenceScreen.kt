package com.hulkdx.core5x5.screenshottest

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// Both dimensions are divisible by 8, preserving exact 9:16 pixels at the renderer's 420dpi.
@Preview(
    name = "Reference",
    widthDp = 432,
    heightDp = 768,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ReferenceScreen

@Preview(
    name = "Home reference",
    widthDp = 432,
    heightDp = 768,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class HomeReferenceScreen
