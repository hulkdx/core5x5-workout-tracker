package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
fun Core5x5SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = Core5x5Dimensions.SettingHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = Core5x5Typography.Body,
            color = Core5x5Colors.PrimaryText,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Core5x5Dimensions.SettingValueGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = Core5x5Typography.Caption,
                color = Core5x5Colors.SecondaryText,
                textAlign = TextAlign.End,
            )
            Text(
                text = "›",
                modifier = Modifier.clearAndSetSemantics {},
                style = Core5x5Typography.Body,
                color = Core5x5Colors.MutedText,
            )
        }
    }
}
