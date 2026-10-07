package com.hulkdx.core5x5.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
internal fun ShellScreen(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onRetryPreferences: () -> Unit = {},
    backgroundColor: Color = Core5x5Colors.Background,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = Core5x5Colors.PrimaryText,
    ) {
        // AppNavigation owns safe insets and reserves the anchored navigation bar.
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTitle) Text(
                text = buildAnnotatedString {
                    val accentIndex = uiState.title.indexOf("5x5")
                    if (accentIndex < 0) {
                        append(uiState.title)
                    } else {
                        append(uiState.title.substring(0, accentIndex))
                        withStyle(SpanStyle(color = Core5x5Colors.Action)) {
                            append(uiState.title.substring(accentIndex))
                        }
                    }
                },
                style = Core5x5Typography.Title,
                color = Core5x5Colors.PrimaryText,
                modifier = Modifier.padding(
                    start = Core5x5Dimensions.ScreenInset,
                    top = Core5x5Dimensions.ContentGap,
                    end = Core5x5Dimensions.ScreenInset,
                    bottom = Core5x5Dimensions.ContentGap,
                ),
            )
            if (uiState.hasPreferencesError) {
                Column(modifier = Modifier.padding(Core5x5Dimensions.ScreenInset)) {
                    Text(
                        text = "Unable to load weight units. Showing ${uiState.weightUnit.symbol}.",
                        style = Core5x5Typography.Caption,
                        color = Core5x5Colors.Destructive,
                    )
                    Core5x5SecondaryButton(label = "Retry preferences", onClick = onRetryPreferences)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                content()
            }
        }
    }
}

@Preview
@Composable
private fun ShellScreenPreview() {
    Core5x5Theme {
        ShellScreen(uiState = ShellUiState()) {
            Text(text = "Today")
        }
    }
}
