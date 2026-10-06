package com.hulkdx.core5x5.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5Tab
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
internal fun ShellScreen(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    selectedTab: Core5x5Tab = Core5x5Tab.TODAY,
    onSelectTab: (Core5x5Tab) -> Unit = {},
    onRetryPreferences: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = ShellBackground,
        contentColor = ShellText,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            if (selectedTab == Core5x5Tab.TODAY) Text(
                text = buildAnnotatedString {
                    val accentIndex = uiState.title.indexOf("5x5")
                    if (accentIndex < 0) {
                        append(uiState.title)
                    } else {
                        append(uiState.title.substring(0, accentIndex))
                        withStyle(SpanStyle(color = ShellAccent)) {
                            append(uiState.title.substring(accentIndex))
                        }
                    }
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = ShellText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 30.sp,
                ),
                modifier = Modifier.padding(
                    start = Core5x5Dimensions.ScreenInset,
                    end = Core5x5Dimensions.ScreenInset,
                    top = Core5x5Dimensions.ContentGap,
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
            Core5x5BottomNavigation(selectedTab = selectedTab, onSelectTab = onSelectTab)
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

private val ShellBackground = Color(0xFF0C1114)
private val ShellAccent = Color(0xFF67E38B)
private val ShellText = Color(0xFFF5F8F7)
