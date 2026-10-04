package com.hulkdx.core5x5.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme

@Composable
internal fun ShellScreen(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = ShellBackground,
        contentColor = ShellText,
    ) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            Text(
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
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 14.dp),
            )
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

private val ShellBackground = Color(0xFF0C1114)
private val ShellAccent = Color(0xFF67E38B)
private val ShellText = Color(0xFFF5F8F7)
