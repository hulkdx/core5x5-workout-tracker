package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.hulkdx.core5x5.core.ui.theme.Core5x5ActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.Core5x5HomeTokens as Home

@Composable
fun Core5x5ExpandedExerciseCard(
    name: String,
    prescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Home.CardRadius),
        color = Home.Card,
        border = BorderStroke(Home.BorderStroke, Home.Border),
    ) {
        Column(modifier = Modifier.padding(horizontal = Active.CardPaddingHorizontal, vertical = Active.CardPaddingVertical)) {
            val headerHeight = with(LocalDensity.current) {
                Active.Exercise.lineHeight.toDp() + Active.Metadata.lineHeight.toDp()
            } + Home.TextGap
            Column(modifier = Modifier.heightIn(min = headerHeight), verticalArrangement = Arrangement.spacedBy(Home.TextGap)) {
                Text(text = name, style = Active.Exercise, color = Home.Primary)
                Text(text = prescription, style = Active.Metadata, color = Home.Secondary)
            }
            Spacer(Modifier.height(Active.SetGap))
            content()
        }
    }
}
