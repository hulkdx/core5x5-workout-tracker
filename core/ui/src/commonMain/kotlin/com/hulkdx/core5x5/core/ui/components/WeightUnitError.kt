package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** Presentation only; the feature owns the observed unit, error, and explicit retry. */
@Composable
fun WeightUnitError(unit: WeightUnit, onRetry: () -> Unit) {
    Column {
        Text(
            text = "Unable to load weight units. Showing ${unit.symbol}.",
            style = AppTypography.Caption,
            color = AppColors.Destructive,
        )
        AppSecondaryButton(label = "Retry preferences", onClick = onRetry)
    }
}
