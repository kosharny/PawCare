package com.example.pawcare.feature.startup

enum class StartupState {
    LOADING,
    READY,
    ERROR,
}

data class StartupUiState(
    val status: StartupState = StartupState.LOADING,
    val progress: Float = 0f,
)
