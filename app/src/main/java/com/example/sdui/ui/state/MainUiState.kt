package com.example.sdui.ui.state

data class MainUiState(
  val connection: ServerConnectionUiState = ServerConnectionUiState.NotChecked,
)

sealed interface ServerConnectionUiState {
  data object NotChecked : ServerConnectionUiState

  data object Checking : ServerConnectionUiState

  data class Connected(
    val status: String,
    val message: String?,
  ) : ServerConnectionUiState

  data class Unavailable(
    val status: String,
    val message: String?,
  ) : ServerConnectionUiState

  data object Failed : ServerConnectionUiState
}
