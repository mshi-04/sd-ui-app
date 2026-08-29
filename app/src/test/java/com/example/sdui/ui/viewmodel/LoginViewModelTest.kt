package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.usecase.SignInUseCase
import com.example.sdui.ui.state.SignInUiState
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
class LoginViewModelTest {
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
  fun normalInitialStateIsIdle() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(Result.success(signedIn()))

    // Act
    // Normal: no interactive flow starts until the screen asks for one
    val actual = viewModel.uiState.value.signIn

    // Assert
    assertEquals(SignInUiState.Idle, actual)
  }

  @Test
  fun flowSignInEmitsInProgressBeforeSucceeded() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(Result.success(signedIn()))

    // Act & Assert
    // Flow: the button stays disabled through the interactive flow before it succeeds
    viewModel.uiState.test {
      assertEquals(SignInUiState.Idle, awaitItem().signIn)
      viewModel.signIn()
      advanceUntilIdle()
      assertEquals(SignInUiState.InProgress, awaitItem().signIn)
      assertEquals(SignInUiState.Succeeded, awaitItem().signIn)
    }
  }

  @Test
  fun errorSignInFailureExposesFailedState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(Result.failure(IOException("offline")))

    // Act
    // Error: a failed or abandoned sign in leaves the screen able to retry
    viewModel.signIn()
    advanceUntilIdle()

    // Assert
    assertEquals(SignInUiState.Failed, viewModel.uiState.value.signIn)
  }

  @Test
  fun boundarySignedOutResultExposesFailedState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(Result.success(AuthSession.SignedOut))

    // Act
    // Boundary: a successful call that produced no account is not a completed sign in
    viewModel.signIn()
    advanceUntilIdle()

    // Assert
    assertEquals(SignInUiState.Failed, viewModel.uiState.value.signIn)
  }

  @Test
  fun interactionSignInWhileInProgressSkipsDuplicateRequest() = runTest(dispatcher) {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.signIn() } returns Result.success(signedIn())
    val viewModel = createViewModel(repository)

    // Act
    // Interaction: a second tap must not open a second interactive flow
    viewModel.signIn()
    viewModel.signIn()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 1) { repository.signIn() }
  }

  private fun createViewModel(result: Result<AuthSession>): LoginViewModel {
    val repository = mockk<AuthRepository>()
    coEvery { repository.signIn() } returns result
    return createViewModel(repository)
  }

  private fun createViewModel(repository: AuthRepository): LoginViewModel =
    LoginViewModel(SignInUseCase(repository))

  private fun signedIn(): AuthSession.SignedIn =
    AuthSession.SignedIn(
      AuthAccount(
        id = AccountId("6f2c1d4e-0000-0000-0000-000000000000"),
        name = AccountName("plain@example.onmicrosoft.com"),
      ),
    )
}
