package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
fun Core5x5PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Core5x5Button(label, onClick, modifier, enabled, primary = true)
}

@Composable
fun Core5x5SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Core5x5Button(label, onClick, modifier, enabled, primary = false)
}

@Composable
private fun Core5x5Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    primary: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        primary && pressed -> Core5x5Colors.ActionPressed
        primary -> Core5x5Colors.Action
        pressed -> Core5x5Colors.Pressed
        else -> Core5x5Colors.Elevated
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = if (primary) Core5x5Colors.Background else Core5x5Colors.PrimaryText,
            disabledContainerColor = Core5x5Colors.Disabled,
            disabledContentColor = Core5x5Colors.DisabledText,
        ),
        contentPadding = PaddingValues(Core5x5Dimensions.ButtonPadding),
        border = if (primary) null else BorderStroke(Core5x5Dimensions.BorderStroke, Core5x5Colors.Border),
        elevation = null,
        interactionSource = interactionSource,
    ) {
        Text(text = label, style = Core5x5Typography.Label, textAlign = TextAlign.Center)
    }
}
