package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
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
import com.hulkdx.core5x5.core.ui.theme.Core5x5RestTokens as Rest

@Composable
fun Core5x5ExpandedExerciseCard(
    name: String,
    prescription: String,
    modifier: Modifier = Modifier,
    resting: Boolean = false,
    onEdit: (() -> Unit)? = null,
    editEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Home.CardRadius),
        color = Home.Card,
        border = BorderStroke(Home.BorderStroke, Home.Border),
    ) {
        val exerciseStyle = if (resting) Rest.Exercise else Active.Exercise
        val metadataStyle = if (resting) Rest.Metadata else Active.Metadata
        Column(modifier = Modifier.padding(
            horizontal = Active.CardPaddingHorizontal,
            vertical = if (resting) Rest.CardPaddingVertical else Active.CardPaddingVertical,
        )) {
            val headerHeight = with(LocalDensity.current) {
                exerciseStyle.lineHeight.toDp() + metadataStyle.lineHeight.toDp()
            } + Home.TextGap
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).heightIn(min = headerHeight), verticalArrangement = Arrangement.spacedBy(Home.TextGap)) {
                    Text(text = name, style = exerciseStyle, color = Home.Primary)
                    Text(text = prescription, style = metadataStyle, color = Home.Secondary)
                }
                if (onEdit != null) TextButton(onClick = onEdit, enabled = editEnabled,
                    modifier = Modifier.heightIn(min = com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions.TouchTargetMin)) {
                    Text("Edit", style = Home.SwitchLabel, color = if (editEnabled) Home.Action else Home.Secondary)
                }
            }
            Spacer(Modifier.height(if (resting) Rest.SetGap else Active.SetGap))
            content()
        }
    }
}
