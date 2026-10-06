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
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

@Composable
fun Core5x5Metrics(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    modifier: Modifier = Modifier,
) {
    // At reference scale the cards measure 78dp inside an 86dp row. The remaining space stays below.
    Column(modifier = modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.MetricsRowHeight)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Core5x5Dimensions.MetricsGap),
        ) {
            Core5x5MetricCard(firstLabel, firstValue, Modifier.weight(1f).fillMaxHeight())
            Core5x5MetricCard(secondLabel, secondValue, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
fun Core5x5MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Core5x5Dimensions.RadiusLarge),
        color = Core5x5Colors.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Core5x5Dimensions.MetricPaddingHorizontal,
                vertical = Core5x5Dimensions.MetricPaddingVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.MetricLabelGap),
        ) {
            Text(text = label, style = Core5x5Typography.Caption, color = Core5x5Colors.SecondaryText)
            Text(text = value, style = Core5x5Typography.Heading, color = Core5x5Colors.PrimaryText)
        }
    }
}
