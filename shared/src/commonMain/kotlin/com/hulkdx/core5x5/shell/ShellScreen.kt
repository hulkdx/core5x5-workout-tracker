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
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppTypography

@Composable
internal fun ShellScreen(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onRetryPreferences: () -> Unit = {},
    backgroundColor: Color = AppColors.Background,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = AppColors.PrimaryText,
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
                        withStyle(SpanStyle(color = AppColors.Action)) {
                            append(uiState.title.substring(accentIndex))
                        }
                    }
                },
                style = AppTypography.Title,
                color = AppColors.PrimaryText,
                modifier = Modifier.padding(
                    start = AppDimensions.ScreenInset,
                    top = AppDimensions.ContentGap,
                    end = AppDimensions.ScreenInset,
                    bottom = AppDimensions.ContentGap,
                ),
            )
            if (uiState.hasPreferencesError) {
                Column(modifier = Modifier.padding(AppDimensions.ScreenInset)) {
                    Text(
                        text = "Unable to load weight units. Showing ${uiState.weightUnit.symbol}.",
                        style = AppTypography.Caption,
                        color = AppColors.Destructive,
                    )
                    AppSecondaryButton(label = "Retry preferences", onClick = onRetryPreferences)
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
    AppTheme {
        ShellScreen(uiState = ShellUiState()) {
            Text(text = "Today")
        }
    }
}
