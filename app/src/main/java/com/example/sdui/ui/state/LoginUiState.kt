package com.example.sdui.ui.state

data class LoginUiState(
  val status: LoginUiStatus = LoginUiStatus.IDLE,
  val accountName: String = "",
)

enum class LoginUiStatus {
  IDLE,
  IN_PROGRESS,
  SUCCEEDED,
  FAILED,
}
