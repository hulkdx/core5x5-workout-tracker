package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5CompleteTokens as Complete

@Composable
fun Core5x5CompletionSuccessIcon() {
    Canvas(Modifier.size(Complete.SuccessSize).clearAndSetSemantics {}) {
        drawCircle(Complete.Action)
        val check = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.50f)
            lineTo(size.width * 0.42f, size.height * 0.68f)
            lineTo(size.width * 0.77f, size.height * 0.31f)
        }
        drawPath(check, Complete.Background, style = Stroke(
            width = size.width * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round,
        ))
    }
}

@Composable
fun Core5x5CompletionMetrics(duration: String, completedSets: String, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardContentWidth = (maxWidth - Complete.MetricsGap) / 2 - Complete.CardPadding * 2
        val requiredTextWidth = maxOf(
            textMeasurer.measure(AnnotatedString("Duration"), Complete.Label).size.width,
            textMeasurer.measure(AnnotatedString("Completed sets"), Complete.Label).size.width,
            textMeasurer.measure(AnnotatedString(duration), Complete.MetricValue).size.width,
            textMeasurer.measure(AnnotatedString(completedSets), Complete.MetricValue).size.width,
        )
        if (requiredTextWidth <= with(density) { cardContentWidth.toPx() }) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Complete.MetricsGap),
            ) {
                CompletionMetric("Duration", duration, Modifier.weight(1f).fillMaxHeight())
                CompletionMetric("Completed sets", completedSets, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Complete.MetricsGap)) {
                CompletionMetric("Duration", duration, Modifier.fillMaxWidth())
                CompletionMetric("Completed sets", completedSets, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CompletionMetric(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier.heightIn(min = Complete.MetricHeight),
        shape = RoundedCornerShape(Complete.CardRadius), color = Complete.Card,
        border = BorderStroke(Complete.BorderStroke, Complete.Border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Complete.CardPadding, vertical = Complete.MetricPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(Complete.MetricLabelGap),
        ) {
            Text(label, style = Complete.Label, color = Core5x5Colors.SecondaryText)
            Text(value, style = Complete.MetricValue, color = Core5x5Colors.PrimaryText)
        }
    }
}

@Composable
fun Core5x5NextWorkoutSummary(title: String, indicator: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = Complete.NextHeight),
        shape = RoundedCornerShape(Complete.CardRadius), color = Complete.Card,
        border = BorderStroke(Complete.BorderStroke, Complete.Border),
    ) {
        Row(
            modifier = Modifier.padding(Complete.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(Complete.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Complete.NextLabelGap)) {
                Text("Next up", style = Complete.Label, color = Core5x5Colors.SecondaryText)
                Text(title, style = Complete.NextTitle, color = Core5x5Colors.PrimaryText)
            }
            Surface(shape = RoundedCornerShape(Complete.BadgeRadius), color = Complete.Action) {
                Box(
                    Modifier.sizeIn(minWidth = Complete.BadgeSize, minHeight = Complete.BadgeSize)
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text(indicator, style = Complete.BadgeLabel, color = Complete.Background)
                }
            }
        }
    }
}

@Composable
fun Core5x5CompletionButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = Complete.ButtonHeight),
        shape = RoundedCornerShape(Complete.CardRadius),
        contentPadding = PaddingValues(horizontal = Complete.CardPadding),
        elevation = null,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) Complete.ActionPressed else Complete.Action,
            contentColor = Complete.Background,
        ),
    ) {
        Text(label, style = Complete.ButtonLabel, textAlign = TextAlign.Center)
    }
}
