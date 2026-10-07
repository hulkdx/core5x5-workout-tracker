package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
fun Core5x5WorkoutTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    backContentDescription: String = "Back to Today",
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.sizeIn(
                minWidth = Core5x5Dimensions.TouchTargetMin,
                minHeight = Core5x5Dimensions.TouchTargetMin,
            ).clickable(enabled = backEnabled, role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backContentDescription },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "‹",
                modifier = Modifier.clearAndSetSemantics {},
                style = Core5x5Typography.Title,
                color = Core5x5Colors.PrimaryText,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = Core5x5Typography.Label,
            color = Core5x5Colors.PrimaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(Core5x5Dimensions.TouchTargetMin))
    }
}
