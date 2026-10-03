package com.example.myapplication.shell

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ShellViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()
}
