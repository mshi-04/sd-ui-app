package com.example.sdui.ui.state

sealed interface SessionUiState {
  data object Restoring : SessionUiState

  data object SignedOut : SessionUiState

  data class SignedIn(
    val accountName: String,
  ) : SessionUiState
}
