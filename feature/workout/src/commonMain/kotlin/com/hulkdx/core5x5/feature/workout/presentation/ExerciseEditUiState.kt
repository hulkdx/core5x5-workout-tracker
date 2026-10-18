package com.hulkdx.core5x5.feature.workout.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit

data class ExerciseEditUiState(
    val position: Int,
    val exerciseName: String,
    val weightKg: Double,
    val unit: WeightUnit,
    val sets: Int,
    val reps: Int,
    val restDurationMillis: Long?,
    val effectiveRestDurationMillis: Long,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val showCustomRest: Boolean = false,
    val customRestSeconds: Long = 180,
) {
    val canSave: Boolean get() = !isSaving && !showCustomRest
    fun toEdit() = ExerciseEdit(weightKg, sets, reps, restDurationMillis)
}
