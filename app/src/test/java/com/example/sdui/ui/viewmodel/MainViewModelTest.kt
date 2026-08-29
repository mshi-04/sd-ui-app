package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import com.example.sdui.domain.repository.ServerStatusRepository
import com.example.sdui.domain.usecase.CheckServerStatusUseCase
import com.example.sdui.ui.state.ServerConnectionUiState
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
class MainViewModelTest {
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
  fun normalInitialStateIsNotChecked() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeServerStatusRepository(Result.success(okStatus())))

    // Act
    // Normal: no request is made until the screen asks for one
    val actual = viewModel.uiState.value.connection

    // Assert
    assertEquals(ServerConnectionUiState.NotChecked, actual)
  }

  @Test
  fun flowCheckConnectionEmitsCheckingBeforeConnected() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(FakeServerStatusRepository(Result.success(okStatus())))

    // Act & Assert
    // Flow: the UI state passes through checking before reaching connected
    viewModel.uiState.test {
      assertEquals(ServerConnectionUiState.NotChecked, awaitItem().connection)
      viewModel.checkConnection()
      advanceUntilIdle()
      assertEquals(ServerConnectionUiState.Checking, awaitItem().connection)
      assertEquals(
        ServerConnectionUiState.Connected(status = "ok", message = "ready"),
        awaitItem().connection,
      )
    }
  }

  @Test
  fun boundaryStatusWithoutMessageExposesNullMessage() = runTest(dispatcher) {
    // Arrange
    val status = serverStatus(code = "ok", message = null)
    val viewModel = createViewModel(FakeServerStatusRepository(Result.success(status)))

    // Act
    // Boundary: a response without a server message leaves the screen to fall back on the status
    viewModel.checkConnection()
    advanceUntilIdle()

    // Assert
    assertEquals(
      ServerConnectionUiState.Connected(status = "ok", message = null),
      viewModel.uiState.value.connection,
    )
  }

  @Test
  fun stateTransitionNonOkStatusExposesUnavailableState() = runTest(dispatcher) {
    // Arrange
    val status = serverStatus(code = "maintenance", message = "try later")
    val viewModel = createViewModel(FakeServerStatusRepository(Result.success(status)))

    // Act
    // StateTransition: an unavailable server keeps its status and message in UI state
    viewModel.checkConnection()
    advanceUntilIdle()

    // Assert
    assertEquals(
      ServerConnectionUiState.Unavailable(status = "maintenance", message = "try later"),
      viewModel.uiState.value.connection,
    )
  }

  @Test
  fun stateTransitionRepositoryFailureExposesFailedState() = runTest(dispatcher) {
    // Arrange
    val failure = Result.failure<ServerStatus>(IOException("offline"))
    val viewModel = createViewModel(FakeServerStatusRepository(failure))

    // Act
    // StateTransition: a failed request is exposed as the failed state
    viewModel.checkConnection()
    advanceUntilIdle()

    // Assert
    assertEquals(ServerConnectionUiState.Failed, viewModel.uiState.value.connection)
  }

  @Test
  fun interactionCheckWhileCheckingSkipsDuplicateRequest() = runTest(dispatcher) {
    // Arrange
    val repository = mockRepositoryReturning(Result.success(okStatus()))
    val viewModel = createViewModel(repository)

    // Act
    // Interaction: a request started while checking does not reach the repository twice
    viewModel.checkConnection()
    viewModel.checkConnection()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 1) { repository.getServerStatus() }
  }

  @Test
  fun interactionCheckAfterCompletionRequestsAgain() = runTest(dispatcher) {
    // Arrange
    val repository = mockRepositoryReturning(Result.success(okStatus()))
    val viewModel = createViewModel(repository)

    // Act
    // Interaction: the guard only blocks while checking, so a finished check can run again
    viewModel.checkConnection()
    advanceUntilIdle()
    viewModel.checkConnection()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 2) { repository.getServerStatus() }
  }

  private fun createViewModel(repository: ServerStatusRepository): MainViewModel =
    MainViewModel(CheckServerStatusUseCase(repository))

  private fun mockRepositoryReturning(result: Result<ServerStatus>): ServerStatusRepository {
    val repository = mockk<ServerStatusRepository>()
    coEvery { repository.getServerStatus() } returns result
    return repository
  }

  private fun okStatus(): ServerStatus = serverStatus(code = "ok", message = "ready")

  private fun serverStatus(code: String, message: String?): ServerStatus =
    ServerStatus(
      code = ServerStatusCode(code),
      message = message?.let { ServerStatusMessage(it) },
    )
}

private class FakeServerStatusRepository(
  private val result: Result<ServerStatus>,
) : ServerStatusRepository {
  override suspend fun getServerStatus(): Result<ServerStatus> = result
}
