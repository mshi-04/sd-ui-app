package com.example.sdui.domain.model

sealed interface AuthSession {
  data class SignedIn(
    val account: AuthAccount,
  ) : AuthSession

  data object SignedOut : AuthSession
}
