package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.usecase.RestoreSessionUseCase
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.SessionUiState
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
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
    val viewModel = createViewModel(FakeAuthRepository(Result.success(signedIn())))

    // Act & Assert
    // Flow: the gate stays on the restoring state until the silent restore answers
    viewModel.uiState.test {
      assertEquals(SessionUiState.Restoring, awaitItem())
      advanceUntilIdle()
      assertEquals(SessionUiState.SignedIn(ACCOUNT_NAME), awaitItem())
    }
  }

  @Test
  fun normalRestoreWithoutAccountExposesSignedOutState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeAuthRepository(Result.success(AuthSession.SignedOut)))

    // Act
    // Normal: no cached account means the login screen has to be shown
    advanceUntilIdle()

    // Assert
    assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
  }

  @Test
  fun errorRestoreFailureExposesSignedOutState() = runTest(dispatcher) {
    // Arrange
    val failure = Result.failure<AuthSession>(IOException("offline"))
    val viewModel = createViewModel(FakeAuthRepository(failure))

    // Act
    // Error: a failed silent restore is treated as not signed in rather than as a crash
    advanceUntilIdle()

    // Assert
    assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
  }

  @Test
  fun stateTransitionSignedInNotificationRestoresTheSessionAgain() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(Result.success(AuthSession.SignedOut))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    repository.session = Result.success(signedIn())

    // Act
    // StateTransition: the interactive sign in result is picked up by restoring once more
    viewModel.onSignedIn()
    advanceUntilIdle()

    // Assert
    assertEquals(SessionUiState.SignedIn(ACCOUNT_NAME), viewModel.uiState.value)
  }

  @Test
  fun stateTransitionSignOutLeavesTheSessionSignedOut() = runTest(dispatcher) {
    // Arrange
    val repository = FakeAuthRepository(Result.success(signedIn()))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    // Act
    // StateTransition: signing out drops the session without restoring it again
    viewModel.signOut()
    advanceUntilIdle()

    // Assert
    assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
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

  private fun createViewModel(repository: AuthRepository): SessionViewModel =
    SessionViewModel(
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
  var session: Result<AuthSession>,
) : AuthRepository {
  override suspend fun restoreSession(): Result<AuthSession> = session

  override suspend fun signIn(): Result<AuthSession> = session

  override suspend fun signOut(): Result<Unit> {
    session = Result.success(AuthSession.SignedOut)
    return Result.success(Unit)
  }
}
