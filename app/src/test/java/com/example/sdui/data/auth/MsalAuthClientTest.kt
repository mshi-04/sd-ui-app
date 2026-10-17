package com.example.sdui.data.auth

import android.app.Activity
import com.microsoft.identity.client.CurrentAccountResult
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalClientException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class MsalAuthClientTest {
  private val factory = mockk<MsalApplicationFactory>()
  private val application = mockk<ISingleAccountPublicClientApplication>()
  private val activity = mockk<Activity>()
  private val holder = SignInActivityHolder()
  private val parameters = slot<SignInParameters>()
  private val account = mockk<IAccount> {
    every { id } returns "account-id"
    every { username } returns "user@example.com"
    every { authority } returns "https://login.microsoftonline.com/tenant"
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun interactionConcurrentOperationsReuseOneApplication() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(null)

    // Act
    // Interaction: concurrent initialization requests share the created application
    val firstSignIn = async { client.signIn() }
    val secondSignIn = async { client.signIn() }
    firstSignIn.await()
    secondSignIn.await()
    client.signOut()

    // Assert
    verify(exactly = 1) { factory.create() }
  }

  @Test
  fun errorFailedInitializationCanBeRetried() = runTest {
    // Arrange
    val client = createClient()
    val error = MsalClientException("creation_failed")
    every { factory.create() } throws error andThen application
    currentAccount(null)

    // Act
    // Error: failed creation is not cached and the next operation retries
    val failure = runCatching { client.signOut() }.exceptionOrNull()
    client.signOut()

    // Assert
    assertAll(
      { assertSame(error, failure) },
      { verify(exactly = 2) { factory.create() } },
    )
  }

  @Test
  fun normalSignInMapsTheSuccessCallbackAccount() = runTest {
    // Arrange
    val client = createClient()
    holder.attach(activity)
    every { application.signIn(capture(parameters)) } answers {
      parameters.captured.callback.onSuccess(authenticationResult(account))
    }

    // Act
    // Normal: successful interactive authentication returns the account identity
    val actual = client.signIn()

    // Assert
    assertAll(
      { assertEquals(MsalAccount("account-id", "user@example.com"), actual) },
      { assertSame(activity, parameters.captured.activity) },
      { assertEquals(listOf(API_SCOPE), parameters.captured.scopes) },
    )
  }

  @Test
  fun boundarySignInWithoutActivitySkipsInteractiveAuthentication() = runTest {
    // Arrange
    val client = createClient()

    // Act
    // Boundary: sign-in cannot launch without an attached activity
    val actual = client.signIn()

    // Assert
    assertAll(
      { assertNull(actual) },
      { verify(exactly = 0) { application.signIn(any<SignInParameters>()) } },
    )
  }

  @Test
  fun errorSignInPropagatesTheErrorCallback() = runTest {
    // Arrange
    val client = createClient()
    holder.attach(activity)
    val error = MsalClientException("sign_in_failed")
    every { application.signIn(capture(parameters)) } answers {
      parameters.captured.callback.onError(error)
    }

    // Act
    // Error: the callback exception is propagated without replacement
    val actual = runCatching { client.signIn() }.exceptionOrNull()

    // Assert
    assertSame(error, actual)
  }

  @Test
  fun boundarySignInCancelCallbackReturnsNoAccount() = runTest {
    // Arrange
    val client = createClient()
    holder.attach(activity)
    every { application.signIn(capture(parameters)) } answers {
      parameters.captured.callback.onCancel()
    }

    // Act
    // Boundary: dismissing interactive authentication produces no session
    val actual = client.signIn()

    // Assert
    assertNull(actual)
  }

  @ParameterizedTest
  @EnumSource(LateCallback::class)
  fun coroutineLateCallbackCannotCompleteCancelledSignIn(callback: LateCallback) = runTest {
    // Arrange
    val client = createClient()
    holder.attach(activity)
    every { application.signIn(capture(parameters)) } returns Unit
    var completed = false
    val operation = launch {
      client.signIn()
      completed = true
    }
    runCurrent()

    // Act
    // Coroutine: a callback after cancellation cannot publish a signed-in result
    operation.cancel()
    when (callback) {
      LateCallback.Success -> parameters.captured.callback.onSuccess(authenticationResult(account))
      LateCallback.Error -> parameters.captured.callback.onError(MsalClientException("late_error"))
      LateCallback.Cancel -> parameters.captured.callback.onCancel()
    }
    operation.join()

    // Assert
    assertAll(
      { assertTrue(operation.isCancelled) },
      { assertEquals(false, completed) },
    )
  }

  @Test
  fun normalSignOutRemovesTheCachedAccount() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(account)
    every { application.signOut() } returns true

    // Act
    // Normal: a cached account is signed out through MSAL
    client.signOut()

    // Assert
    verify(exactly = 1) { application.signOut() }
  }

  @Test
  fun boundarySignOutWithoutAccountDoesNothing() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(null)

    // Act
    // Boundary: an already signed-out application skips account removal
    client.signOut()

    // Assert
    verify(exactly = 0) { application.signOut() }
  }

  @Test
  fun errorSignOutPropagatesMsalFailure() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(account)
    val error = MsalClientException("sign_out_failed")
    every { application.signOut() } throws error

    // Act
    // Error: account removal errors remain visible to the repository
    val actual = runCatching { client.signOut() }.exceptionOrNull()

    // Assert
    assertSame(error, actual)
  }

  @Test
  fun boundarySignOutReportedAsNotRemovedWhileAccountIsGoneSucceeds() = runTest {
    // Arrange
    val client = createClient()
    every { application.currentAccount } returnsMany
      listOf(
        CurrentAccountResult(account, null, false),
        CurrentAccountResult(null, null, false),
      )
    every { application.signOut() } returns false

    // Act
    // Boundary: a missing token cache record is not a failure once the account is cleared
    val failure = runCatching { client.signOut() }.exceptionOrNull()

    // Assert
    assertAll(
      { assertNull(failure) },
      { verify(exactly = 1) { application.signOut() } },
    )
  }

  @Test
  fun errorSignOutReportedAsNotRemovedWhileAccountRemainsFails() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(account)
    every { application.signOut() } returns false

    // Act
    // Error: a cached account that survives sign-out would be restored on the next launch
    val actual = runCatching { client.signOut() }.exceptionOrNull()

    // Assert
    assertInstanceOf(IllegalStateException::class.java, actual)
  }

  @Test
  fun coroutineSignOutPropagatesCancellation() = runTest {
    // Arrange
    val client = createClient()
    currentAccount(account)
    val cancellation = CancellationException("cancelled")
    every { application.signOut() } throws cancellation

    // Act
    // Coroutine: coroutine cancellation is not converted into a sign-out result
    val actual = runCatching { client.signOut() }.exceptionOrNull()

    // Assert
    assertInstanceOf(CancellationException::class.java, actual)
  }

  private fun TestScope.createClient(): MsalAuthClient {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    every { factory.create() } returns application
    return MsalAuthClient(factory, API_SCOPE, holder, dispatcher)
  }

  private fun currentAccount(current: IAccount?) {
    every { application.currentAccount } returns CurrentAccountResult(current, null, false)
  }

  private fun authenticationResult(current: IAccount): IAuthenticationResult = mockk {
    every { account } returns current
  }

  private companion object {
    const val API_SCOPE = "api://test/UiSchema.Read"
  }

  enum class LateCallback {
    Success,
    Error,
    Cancel,
  }
}
