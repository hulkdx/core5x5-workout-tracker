package com.hulkdx.core5x5.screenshottest

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// One normal-size screenshot per screen at the local design handoff's reference viewport.
@Preview(name = "Reference", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class ReferenceScreen
