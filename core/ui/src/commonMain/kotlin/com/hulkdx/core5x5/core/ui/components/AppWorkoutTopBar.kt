package com.hulkdx.core5x5.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens as Home
import com.hulkdx.core5x5.core.ui.theme.AppTypography

@Composable
fun AppWorkoutTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    backContentDescription: String = "Back to Today",
    refined: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.testTag("workout-back").sizeIn(
                minWidth = AppDimensions.TouchTargetMin,
                minHeight = AppDimensions.TouchTargetMin,
            ).clickable(enabled = backEnabled, role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backContentDescription },
            contentAlignment = Alignment.CenterStart,
        ) {
            if (refined) Icon(
                imageVector = BackIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Home.Primary,
            ) else Text(
                text = "‹",
                modifier = Modifier.clearAndSetSemantics {},
                style = AppTypography.Title,
                color = AppColors.PrimaryText,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).testTag("workout-title"),
            style = if (refined) Home.Button else AppTypography.Label,
            color = if (refined) Home.Primary else AppColors.PrimaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(AppDimensions.TouchTargetMin))
    }
}

private val BackIcon = ImageVector.Builder(
    name = "Back", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
    addPath(pathData = addPathNodes("M15 4l-8 8 8 8"), fill = null, stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
}.build()
