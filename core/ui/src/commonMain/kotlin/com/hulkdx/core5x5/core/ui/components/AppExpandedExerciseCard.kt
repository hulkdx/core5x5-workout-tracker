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
import com.hulkdx.core5x5.core.ui.theme.AppActiveTokens as Active
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home

@Composable
fun AppExpandedExerciseCard(
    name: String,
    prescription: String,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    editEnabled: Boolean = true,
    dimEditWhenDisabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Home.CardRadius),
        color = Home.Card,
        border = BorderStroke(Home.BorderStroke, Home.Border),
    ) {
        val exerciseStyle = Active.Exercise
        val metadataStyle = Active.Metadata
        Column(modifier = Modifier.padding(
            horizontal = Active.CardPaddingHorizontal,
            vertical = Active.CardPaddingVertical,
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
                    modifier = Modifier.heightIn(min = com.hulkdx.core5x5.core.ui.theme.AppDimensions.TouchTargetMin)) {
                    Text("Edit", style = Home.SwitchLabel,
                        color = if (editEnabled || !dimEditWhenDisabled) Home.Action else Home.Secondary)
                }
            }
            Spacer(Modifier.height(Active.SetGap))
            content()
        }
    }
}
