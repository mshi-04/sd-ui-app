package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.usecase.RestoreSessionUseCase
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.AuthUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
  fun flowRestoredSessionEmitsRestoringBeforeSignedIn() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(restoreResult = Result.success(signedIn())))

    // Act & Assert
    // Flow: the app stays on the restoring screen until the silent restore answers
    viewModel.uiState.test {
      assertEquals(AuthUiState.Restoring, awaitItem())
      advanceUntilIdle()
      assertEquals(AuthUiState.SignedIn(ACCOUNT_NAME), awaitItem())
    }
  }

  @Test
  fun normalRestoreWithoutAccountExposesSignedOutState() = runTest(dispatcher) {
    // Arrange
    val viewModel =
      createViewModel(FakeAuthRepository(restoreResult = Result.success(AuthSession.SignedOut)))

    // Act
    // Normal: no cached account means the login screen has to be shown
    advanceUntilIdle()

    // Assert
    assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
  }

  @Test
  fun errorRestoreFailureExposesSignedOutState() = runTest(dispatcher) {
    // Arrange
    val repository =
      FakeAuthRepository(restoreResult = Result.failure(IOException("offline")))
    val viewModel = createViewModel(repository)

    // Act
    // Error: a failed silent restore is treated as not signed in rather than as a crash
    advanceUntilIdle()

    // Assert
    assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
  }

  @Test
  fun stateTransitionSignedInNotificationExposesSignedInState() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(restoreResult = Result.success(AuthSession.SignedOut))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // StateTransition: the login result opens the dashboard with its account name
    val accepted = viewModel.onSignedIn(ACCOUNT_NAME)

    // Assert
    assertAll(
      { assertEquals(true, accepted) },
      { assertEquals(AuthUiState.SignedIn(ACCOUNT_NAME), viewModel.uiState.value) },
    )
  }

  @Test
  fun boundarySignedInNotificationWhileAlreadySignedInIsRejected() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(restoreResult = Result.success(signedIn())))
    advanceUntilIdle()

    // Act
    // Boundary: a stale login result cannot replace the active account
    val accepted = viewModel.onSignedIn("other@example.onmicrosoft.com")

    // Assert
    assertAll(
      { assertEquals(false, accepted) },
      { assertEquals(AuthUiState.SignedIn(ACCOUNT_NAME), viewModel.uiState.value) },
    )
  }

  @Test
  fun interactionSignedInNotificationDoesNotRestoreSessionAgain() = runTest(dispatcher) {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.restoreSession() } returns Result.success(AuthSession.SignedOut)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // Interaction: the login result supplies the account without another silent restore
    viewModel.onSignedIn(ACCOUNT_NAME)

    // Assert
    coVerify(exactly = 1) { repository.restoreSession() }
  }

  @Test
  fun flowFailedSignOutEmitsRestoringBeforeSignedOut() = runTest(dispatcher) {
    // Arrange
    val repository =
      FakeAuthRepository(
        restoreResult = Result.success(signedIn()),
        signOutResult = Result.failure(IOException("offline")),
      )
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act & Assert
    // Flow: signing out returns to login even when the repository cannot clear the session
    viewModel.uiState.test {
      assertEquals(AuthUiState.SignedIn(ACCOUNT_NAME), awaitItem())
      viewModel.signOut()
      advanceUntilIdle()
      assertEquals(AuthUiState.Restoring, awaitItem())
      assertEquals(AuthUiState.SignedOut, awaitItem())
    }
  }

  @Test
  fun interactionSignOutWhileSignedOutIsIgnored() = runTest(dispatcher) {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.restoreSession() } returns Result.success(AuthSession.SignedOut)
    coEvery { repository.signOut() } returns Result.success(Unit)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // Interaction: an unauthorized response after signing out must not sign out twice
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 0) { repository.signOut() }
  }

  private fun createViewModel(repository: AuthRepository): AuthViewModel =
    AuthViewModel(
      restoreSessionUseCase = RestoreSessionUseCase(repository),
      signOutUseCase = SignOutUseCase(repository),
    )

  private fun signedIn(): AuthSession.SignedIn =
    AuthSession.SignedIn(
      AuthAccount(
        id = AccountId("6f2c1d4e-0000-0000-0000-000000000000"),
        name = AccountName(ACCOUNT_NAME),
      ),
    )

  private companion object {
    const val ACCOUNT_NAME = "plain@example.onmicrosoft.com"
  }
}

private class FakeAuthRepository(
  var restoreResult: Result<AuthSession>,
  var signOutResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
  override suspend fun restoreSession(): Result<AuthSession> = restoreResult

  override suspend fun signIn(): Result<AuthSession> = restoreResult

  override suspend fun signOut(): Result<Unit> = signOutResult
}
