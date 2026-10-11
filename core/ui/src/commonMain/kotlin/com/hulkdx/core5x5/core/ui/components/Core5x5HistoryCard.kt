package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.hulkdx.core5x5.core.ui.theme.Core5x5HistoryTokens as History

/** An informational saved-session card with grouped accessibility content. */
@Composable
fun Core5x5HistoryCard(
    weekday: String,
    day: String,
    fullDate: String,
    title: String,
    summary: String,
    duration: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(History.CardRadius),
        color = History.Card,
        border = BorderStroke(History.BorderStroke, History.Border),
    ) {
        Row(
            modifier = Modifier.heightIn(min = History.CardHeight).padding(History.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryDateTile(weekday, day, fullDate)
            Column(
                modifier = Modifier.weight(1f).padding(start = History.DateContentGap),
            ) {
                Text(title, style = History.Workout, color = History.Primary)
                Column(
                    modifier = Modifier.padding(top = History.TitleSummaryGap),
                    verticalArrangement = Arrangement.spacedBy(History.TextGap),
                ) {
                    Text(summary, style = History.Metadata, color = History.Secondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(History.ClockGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HistoryClockIcon()
                        Text(duration, style = History.Metadata, color = History.Secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDateTile(weekday: String, day: String, fullDate: String) {
    Surface(shape = RoundedCornerShape(History.DateRadius), color = History.DateTile) {
        Column(
            modifier = Modifier
                .widthIn(min = History.DateWidth)
                .heightIn(min = History.DateHeight)
                .padding(horizontal = History.CardPadding, vertical = History.DateGap)
                .clearAndSetSemantics { contentDescription = fullDate },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(History.DateGap, Alignment.CenterVertically),
        ) {
            Text(weekday, style = History.Weekday, color = History.Secondary, textAlign = TextAlign.Center)
            Text(day, style = History.Day, color = History.Primary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HistoryClockIcon() {
    Canvas(Modifier.size(History.ClockSize).clearAndSetSemantics {}) {
        val stroke = size.width * 0.08f
        drawCircle(History.Secondary, radius = size.width / 2 - stroke, style = Stroke(stroke))
        drawLine(History.Secondary, center, Offset(center.x, size.height * 0.24f), stroke, StrokeCap.Round)
        drawLine(History.Secondary, center, Offset(size.width * 0.69f, size.height * 0.62f), stroke, StrokeCap.Round)
    }
}
