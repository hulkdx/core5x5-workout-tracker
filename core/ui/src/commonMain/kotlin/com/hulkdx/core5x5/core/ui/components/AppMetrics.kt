package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppTypography

@Composable
fun AppMetrics(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    modifier: Modifier = Modifier,
) {
    // At reference scale the cards measure 78dp inside an 86dp row. The remaining space stays below.
    Column(modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.MetricsRowHeight)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.MetricsGap),
        ) {
            AppMetricCard(firstLabel, firstValue, Modifier.weight(1f).fillMaxHeight())
            AppMetricCard(secondLabel, secondValue, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
fun AppMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.RadiusLarge),
        color = AppColors.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AppDimensions.MetricPaddingHorizontal,
                vertical = AppDimensions.MetricPaddingVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.MetricLabelGap),
        ) {
            Text(text = label, style = AppTypography.Caption, color = AppColors.SecondaryText)
            Text(text = value, style = AppTypography.Heading, color = AppColors.PrimaryText)
        }
    }
}
