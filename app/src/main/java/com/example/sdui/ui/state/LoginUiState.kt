package com.example.sdui.ui.state

data class LoginUiState(
  val signIn: SignInUiState = SignInUiState.Idle,
)

sealed interface SignInUiState {
  data object Idle : SignInUiState

  data object InProgress : SignInUiState

  data class Succeeded(
    val accountName: String,
  ) : SignInUiState

  data object Failed : SignInUiState
}
