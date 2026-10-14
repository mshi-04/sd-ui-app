package com.example.sdui.ui.state

data class AuthUiState(
  val status: AuthUiStatus = AuthUiStatus.SIGNING_OUT,
  val accountName: String = "",
)

enum class AuthUiStatus {
  SIGNING_OUT,
  SIGN_OUT_FAILED,
  SIGNED_OUT,
  SIGNED_IN,
}
