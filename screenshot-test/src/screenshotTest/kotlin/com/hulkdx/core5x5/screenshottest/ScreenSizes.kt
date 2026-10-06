package com.hulkdx.core5x5.screenshottest

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// Reference/narrow/text-growth dimensions follow the local design handoff.
// The nine compact/medium/expanded combinations follow the testing-setup skill.
@Preview(name = "Reference", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Narrow", widthDp = 320, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Larger text", widthDp = 390, heightDp = 844, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Compact short", widthDp = 400, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Compact medium", widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Compact tall", widthDp = 400, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Medium short", widthDp = 610, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Medium medium", widthDp = 610, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Medium tall", widthDp = 610, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Expanded short", widthDp = 900, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Expanded medium", widthDp = 900, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Expanded tall", widthDp = 900, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class ScreenSizes

@Preview(widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class ReferenceScreen
