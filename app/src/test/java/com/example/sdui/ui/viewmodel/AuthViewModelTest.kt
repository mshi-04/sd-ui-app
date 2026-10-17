package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.repository.UiSchemaRepository
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.AuthUiState
import com.example.sdui.ui.state.AuthUiStatus
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun flowStartupSignOutEmitsSigningOutBeforeSignedOut() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(Result.success(Unit)))

    // Act & Assert
    // Flow: the app clears any leftover session before showing the login screen
    viewModel.uiState.test {
      assertEquals(AuthUiState(status = AuthUiStatus.SIGNING_OUT), awaitItem())
      advanceUntilIdle()
      assertEquals(AuthUiState(status = AuthUiStatus.SIGNED_OUT), awaitItem())
    }
  }

  @Test
  fun errorStartupSignOutFailureExposesSignOutFailedState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(Result.failure(IOException("offline"))))

    // Act
    // Error: a leftover session that cannot be cleared must not reach the login screen
    advanceUntilIdle()

    // Assert
    assertEquals(AuthUiState(status = AuthUiStatus.SIGN_OUT_FAILED), viewModel.uiState.value)
  }

  @Test
  fun stateTransitionRetryAfterStartupFailureExposesSignedOutState() = runTest(dispatcher) {
    // Arrange
    val repository =
      FakeAuthRepository(Result.failure(IOException("offline")), Result.success(Unit))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // StateTransition: retrying from the failure screen clears the session and opens login
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    assertAll(
      { assertEquals(AuthUiState(status = AuthUiStatus.SIGNED_OUT), viewModel.uiState.value) },
      { assertEquals(2, repository.signOutCount) },
    )
  }

  @Test
  fun stateTransitionRepeatedFailureStaysOnSignOutFailedState() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(Result.failure(IOException("offline")))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // StateTransition: a failed retry returns to the retry screen instead of the login screen
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    assertAll(
      { assertEquals(AuthUiState(status = AuthUiStatus.SIGN_OUT_FAILED), viewModel.uiState.value) },
      { assertEquals(2, repository.signOutCount) },
    )
  }

  @Test
  fun stateTransitionSignedInNotificationExposesSignedInState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(Result.success(Unit)))
    advanceUntilIdle()

    // Act
    // StateTransition: the login result opens the dashboard with its account name
    val accepted = viewModel.onSignedIn(ACCOUNT_NAME)

    // Assert
    assertAll(
      { assertEquals(true, accepted) },
      { assertEquals(signedInState(), viewModel.uiState.value) },
    )
  }

  @Test
  fun boundarySignedInNotificationWhileAlreadySignedInIsRejected() = runTest(dispatcher) {
    // Arrange
    val viewModel = createSignedInViewModel(FakeAuthRepository(Result.success(Unit)))

    // Act
    // Boundary: a stale login result cannot replace the active account
    val accepted = viewModel.onSignedIn("other@example.onmicrosoft.com")

    // Assert
    assertAll(
      { assertEquals(false, accepted) },
      { assertEquals(signedInState(), viewModel.uiState.value) },
    )
  }

  @Test
  fun boundarySignedInNotificationWhileSignOutFailedIsRejected() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(Result.failure(IOException("offline"))))
    advanceUntilIdle()

    // Act
    // Boundary: protected content stays hidden until the leftover session is cleared
    val accepted = viewModel.onSignedIn(ACCOUNT_NAME)

    // Assert
    assertAll(
      { assertEquals(false, accepted) },
      { assertEquals(AuthUiState(status = AuthUiStatus.SIGN_OUT_FAILED), viewModel.uiState.value) },
    )
  }

  @Test
  fun flowSignOutEmitsSigningOutBeforeSignedOut() = runTest(dispatcher) {
    // Arrange
    val viewModel = createSignedInViewModel(FakeAuthRepository(Result.success(Unit)))

    // Act & Assert
    // Flow: a completed sign-out passes through the progress state before login
    viewModel.uiState.test {
      assertEquals(signedInState(), awaitItem())
      viewModel.signOut()
      advanceUntilIdle()
      assertEquals(AuthUiState(status = AuthUiStatus.SIGNING_OUT), awaitItem())
      assertEquals(AuthUiState(status = AuthUiStatus.SIGNED_OUT), awaitItem())
    }
  }

  @Test
  fun flowFailedSignOutEmitsSigningOutBeforeSignOutFailed() = runTest(dispatcher) {
    // Arrange
    val viewModel =
      createSignedInViewModel(
        FakeAuthRepository(Result.success(Unit), Result.failure(IOException("offline"))),
      )

    // Act & Assert
    // Flow: a failed sign-out never reports SIGNED_OUT
    viewModel.uiState.test {
      assertEquals(signedInState(), awaitItem())
      viewModel.signOut()
      advanceUntilIdle()
      assertEquals(AuthUiState(status = AuthUiStatus.SIGNING_OUT), awaitItem())
      assertEquals(AuthUiState(status = AuthUiStatus.SIGN_OUT_FAILED), awaitItem())
    }
  }

  @Test
  fun interactionSignOutWhileSigningOutIsIgnored() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(Result.success(Unit))
    val viewModel = createSignedInViewModel(repository)
    repository.signOutGate = CompletableDeferred()
    viewModel.signOut()
    advanceUntilIdle()

    // Act
    // Interaction: a second request while a sign-out is running must not start another one
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    assertAll(
      { assertEquals(2, repository.signOutCount) },
      { assertEquals(AuthUiState(status = AuthUiStatus.SIGNING_OUT), viewModel.uiState.value) },
    )
  }

  @Test
  fun interactionSignOutWhileSignedOutIsIgnored() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(Result.success(Unit))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // Interaction: an unauthorized response after signing out must not sign out twice
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    assertAll(
      { assertEquals(1, repository.signOutCount) },
      { assertEquals(AuthUiState(status = AuthUiStatus.SIGNED_OUT), viewModel.uiState.value) },
    )
  }

  private fun createViewModel(repository: AuthRepository): AuthViewModel =
    AuthViewModel(
      signOutUseCase = SignOutUseCase(repository, mockk<UiSchemaRepository>(relaxed = true)),
    )

  private fun TestScope.createSignedInViewModel(
    repository: AuthRepository,
  ): AuthViewModel {
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    viewModel.onSignedIn(ACCOUNT_NAME)
    return viewModel
  }

  private fun signedInState(): AuthUiState =
    AuthUiState(status = AuthUiStatus.SIGNED_IN, accountName = ACCOUNT_NAME)

  private companion object {
    const val ACCOUNT_NAME = "plain@example.onmicrosoft.com"
  }
}

private class FakeAuthRepository(
  private val signOutResults: List<Result<Unit>>,
) : AuthRepository {
  constructor(only: Result<Unit>) : this(listOf(only))

  constructor(first: Result<Unit>, second: Result<Unit>) : this(listOf(first, second))


  var signOutCount = 0
    private set
  var signOutGate: CompletableDeferred<Unit>? = null

  override suspend fun signIn(): Result<AuthSession> = Result.success(AuthSession.SignedOut)

  override suspend fun signOut(): Result<Unit> {
    signOutCount++
    signOutGate?.await()
    return signOutResults[minOf(signOutCount, signOutResults.size) - 1]
  }
}
