package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.resources.Res
import com.hulkdx.core5x5.feature.workout.resources.bench_press
import com.hulkdx.core5x5.feature.workout.resources.bent_over_row
import com.hulkdx.core5x5.feature.workout.resources.deadlift
import com.hulkdx.core5x5.feature.workout.resources.overhead_press
import com.hulkdx.core5x5.feature.workout.resources.squat
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ExerciseVisual(exercise: Exercise, compact: Boolean, modifier: Modifier = Modifier) {
    val resource = when (exercise) {
        Exercise.SQUAT -> Res.drawable.squat
        Exercise.BENCH_PRESS -> Res.drawable.bench_press
        Exercise.BARBELL_ROW -> Res.drawable.bent_over_row
        Exercise.OVERHEAD_PRESS -> Res.drawable.overhead_press
        Exercise.DEADLIFT -> Res.drawable.deadlift
    }
    val height = if (compact) Core5x5Dimensions.RestVisualHeight else Core5x5Dimensions.ActiveVisualHeight
    val shape = RoundedCornerShape(Core5x5Dimensions.RadiusLarge)
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(shape).background(Core5x5Colors.Subtle),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            modifier = (if (compact) Modifier.widthIn(max = Core5x5Dimensions.RestImageWidth) else Modifier)
                .fillMaxWidth().height(height).clip(shape),
            contentScale = ContentScale.Crop,
        )
    }
}
