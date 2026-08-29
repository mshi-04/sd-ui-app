package com.example.sdui.data.auth

import android.content.Context
import com.example.sdui.R
import com.example.sdui.di.ApiScope
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class MsalAuthClient @Inject constructor(
  @param:ApplicationContext private val context: Context,
  @param:ApiScope private val apiScope: String,
  private val signInActivityHolder: SignInActivityHolder,
) : AccessTokenProvider {
  private val applicationLock = Mutex()

  @Volatile
  private var application: ISingleAccountPublicClientApplication? = null

  suspend fun restoreSignedInAccount(): MsalAccount? =
    withContext(Dispatchers.IO) {
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
    withContext(Dispatchers.IO) {
      val client = requireApplication()
      if (client.currentAccount.currentAccount != null) {
        client.signOut()
      }
    }
  }

  override fun accessTokenOrNull(): String? = runBlocking { silentAccessTokenOrNull() }

  private suspend fun silentAccessTokenOrNull(): String? =
    withContext(Dispatchers.IO) {
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
      application ?: withContext(Dispatchers.IO) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
          context,
          R.raw.msal_config,
        )
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
