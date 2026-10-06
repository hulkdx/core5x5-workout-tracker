package com.hulkdx.core5x5.shell

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit

internal data class ShellUiState(
    val title: String = "Core5x5",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val hasPreferencesError: Boolean = false,
)
