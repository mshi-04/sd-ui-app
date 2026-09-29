package com.example.sdui.ui.state

data class AuthUiState(
  val status: AuthUiStatus = AuthUiStatus.RESTORING,
  val accountName: String = "",
)

enum class AuthUiStatus {
  RESTORING,
  SIGNED_OUT,
  SIGNED_IN,
}
