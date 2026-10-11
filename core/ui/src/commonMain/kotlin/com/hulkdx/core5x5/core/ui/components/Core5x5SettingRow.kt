package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5SettingsTokens as Settings

/** The parent card owns borders/dividers; the entire row supplies the touch target. */
@Composable
fun Core5x5SettingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = Settings.RowHeight)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Settings.CardInset, vertical = Settings.RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(Settings.ValueGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = Settings.Label,
            color = Settings.Primary,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = Settings.Value,
                color = Settings.Secondary,
                textAlign = TextAlign.End,
            )
        }
        if (onClick != null) {
            Canvas(modifier = Modifier.size(Settings.ChevronSize)) {
                val stroke = Settings.BorderStroke.toPx()
                val top = Offset(size.width * 0.3f, size.height * 0.15f)
                val middle = Offset(size.width * 0.65f, size.height * 0.5f)
                val bottom = Offset(size.width * 0.3f, size.height * 0.85f)
                drawLine(Settings.Secondary, top, middle, stroke, StrokeCap.Round)
                drawLine(Settings.Secondary, middle, bottom, stroke, StrokeCap.Round)
            }
        }
    }
}
