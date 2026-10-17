package com.example.sdui.data.repository

import com.example.sdui.data.auth.MsalAccount
import com.example.sdui.data.auth.MsalAuthClient
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class DefaultAuthRepository @Inject constructor(
  private val authClient: MsalAuthClient,
) : AuthRepository {
  override suspend fun signIn(): Result<AuthSession> =
    resultOf { authClient.signIn().toSession() }

  override suspend fun signOut(): Result<Unit> = resultOf { authClient.signOut() }

  private suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
    try {
      Result.success(block())
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Result.failure(error)
    }

  private fun MsalAccount?.toSession(): AuthSession {
    if (this == null) return AuthSession.SignedOut
    return AuthSession.SignedIn(
      AuthAccount(
        id = AccountId(id),
        name = AccountName(username),
      ),
    )
  }
}
