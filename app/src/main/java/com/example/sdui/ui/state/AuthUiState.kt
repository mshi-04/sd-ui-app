package com.example.sdui.ui.state

sealed interface AuthUiState {
  data object Restoring : AuthUiState

  data object SignedOut : AuthUiState

  data class SignedIn(
    val accountName: String,
  ) : AuthUiState
}
