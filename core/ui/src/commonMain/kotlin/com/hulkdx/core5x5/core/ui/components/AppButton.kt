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
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTypography

@Composable
fun AppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppButton(label, onClick, modifier, enabled, primary = true)
}

@Composable
fun AppSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppButton(label, onClick, modifier, enabled, primary = false)
}

@Composable
private fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    primary: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        primary && pressed -> AppColors.ActionPressed
        primary -> AppColors.Action
        pressed -> AppColors.Pressed
        else -> AppColors.Elevated
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(AppDimensions.RadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = if (primary) AppColors.Background else AppColors.PrimaryText,
            disabledContainerColor = AppColors.Disabled,
            disabledContentColor = AppColors.DisabledText,
        ),
        contentPadding = PaddingValues(AppDimensions.ButtonPadding),
        border = if (primary) null else BorderStroke(AppDimensions.BorderStroke, AppColors.Border),
        elevation = null,
        interactionSource = interactionSource,
    ) {
        Text(text = label, style = AppTypography.Label, textAlign = TextAlign.Center)
    }
}
