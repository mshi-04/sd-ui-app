package com.example.sdui.data.auth

import com.example.sdui.di.ApiScope
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class MsalAuthClient internal constructor(
  private val applicationFactory: MsalApplicationFactory,
  private val apiScope: String,
  private val signInActivityHolder: SignInActivityHolder,
  private val ioDispatcher: CoroutineDispatcher,
) : AccessTokenProvider {
  @Inject constructor(
    applicationFactory: MsalApplicationFactory,
    @ApiScope apiScope: String,
    signInActivityHolder: SignInActivityHolder,
  ) : this(applicationFactory, apiScope, signInActivityHolder, Dispatchers.IO)

  private val applicationLock = Mutex()

  @Volatile
  private var application: ISingleAccountPublicClientApplication? = null

  suspend fun restoreSignedInAccount(): MsalAccount? =
    withContext(ioDispatcher) {
      val client = requireApplication()
      val account = client.currentAccount.currentAccount ?: return@withContext null
      try {
        client.acquireTokenSilent(silentParametersFor(account)).account.toMsalAccount()
      } catch (error: MsalUiRequiredException) {
        null
      }
    }

  suspend fun signIn(): MsalAccount? {
    val client = requireApplication()
    val activity = signInActivityHolder.currentActivityOrNull() ?: return null

    val result =
      withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
          val parameters =
            SignInParameters.builder()
              .withActivity(activity)
              .withScopes(listOf(apiScope))
              .withCallback(
                object : AuthenticationCallback {
                  override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    continuation.resume(authenticationResult)
                  }

                  override fun onError(exception: MsalException) {
                    continuation.resumeWithException(exception)
                  }

                  override fun onCancel() {
                    continuation.resume(null)
                  }
                },
              )
              .build()
          client.signIn(parameters)
        }
      }
    return result?.account?.toMsalAccount()
  }

  suspend fun signOut() {
    withContext(ioDispatcher) {
      val client = requireApplication()
      if (client.currentAccount.currentAccount != null) {
        client.signOut()
      }
    }
  }

  override fun accessTokenOrNull(): String? = runBlocking { silentAccessTokenOrNull() }

  private suspend fun silentAccessTokenOrNull(): String? =
    withContext(ioDispatcher) {
      try {
        val client = requireApplication()
        val account = client.currentAccount.currentAccount ?: return@withContext null
        client.acquireTokenSilent(silentParametersFor(account)).accessToken
      } catch (error: MsalException) {
        null
      } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        null
      }
    }

  private suspend fun requireApplication(): ISingleAccountPublicClientApplication =
    application ?: applicationLock.withLock {
      application ?: withContext(ioDispatcher) {
        applicationFactory.create()
      }.also { created -> application = created }
    }

  private fun silentParametersFor(account: IAccount): AcquireTokenSilentParameters =
    AcquireTokenSilentParameters.Builder()
      .forAccount(account)
      .fromAuthority(account.authority)
      .withScopes(listOf(apiScope))
      .build()

  private fun IAccount.toMsalAccount(): MsalAccount =
    MsalAccount(id = id, username = username)
}
