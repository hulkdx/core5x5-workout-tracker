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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** Color overrides preserve the shared button's geometry and interaction behavior. */
data class AppButtonPalette(
    val onAction: Color,
    val surface: Color,
    val pressedSurface: Color,
    val primaryText: Color,
    val action: Color,
    val actionPressed: Color,
    val border: Color,
) {
    companion object {
        val Default: AppButtonPalette @Composable get() = AppButtonPalette(
            onAction = AppColors.OnAction,
            surface = AppColors.Elevated,
            pressedSurface = AppColors.Pressed,
            primaryText = AppColors.PrimaryText,
            action = AppColors.Action,
            actionPressed = AppColors.ActionPressed,
            border = AppColors.Border,
        )
        val Home: AppButtonPalette @Composable get() = AppButtonPalette(
            onAction = AppColors.OnAction,
            surface = AppHomeTokens.Card,
            pressedSurface = AppHomeTokens.Border,
            primaryText = AppHomeTokens.Primary,
            action = AppHomeTokens.Action,
            actionPressed = AppHomeTokens.ActionPressed,
            border = AppHomeTokens.Border,
        )
    }
}

@Composable
fun AppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: AppButtonPalette = AppButtonPalette.Default,
) {
    AppButton(label, onClick, modifier, enabled, primary = true, palette = palette)
}

@Composable
fun AppSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: AppButtonPalette = AppButtonPalette.Default,
) {
    AppButton(label, onClick, modifier, enabled, primary = false, palette = palette)
}

@Composable
private fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    primary: Boolean,
    palette: AppButtonPalette,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        primary && pressed -> palette.actionPressed
        primary -> palette.action
        pressed -> palette.pressedSurface
        else -> palette.surface
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(AppDimensions.RadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = if (primary) palette.onAction else palette.primaryText,
            disabledContainerColor = AppColors.Disabled,
            disabledContentColor = AppColors.DisabledText,
        ),
        contentPadding = PaddingValues(AppDimensions.ButtonPadding),
        border = if (primary) null else BorderStroke(AppDimensions.BorderStroke, palette.border),
        elevation = null,
        interactionSource = interactionSource,
    ) {
        Text(text = label, style = AppTypography.Label, textAlign = TextAlign.Center)
    }
}
