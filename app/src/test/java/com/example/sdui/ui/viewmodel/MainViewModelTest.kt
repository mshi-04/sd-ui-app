package com.example.sdui.ui.viewmodel

import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import com.example.sdui.domain.repository.ServerStatusRepository
import com.example.sdui.domain.usecase.CheckServerStatusUseCase
import com.example.sdui.ui.state.ServerConnectionUiState
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
  fun `server response changes connection state`() = runTest(dispatcher) {
    val repository = CountingServerStatusRepository()
    val viewModel = MainViewModel(CheckServerStatusUseCase(repository))

    viewModel.checkConnection()
    advanceUntilIdle()

    assertEquals(
      ServerConnectionUiState.Connected(status = "ok", message = "ready"),
      viewModel.uiState.value.connection,
    )
  }

  @Test
  fun `checking state prevents duplicate request with early return`() = runTest(dispatcher) {
    val repository = CountingServerStatusRepository()
    val viewModel = MainViewModel(CheckServerStatusUseCase(repository))

    viewModel.checkConnection()
    viewModel.checkConnection()
    advanceUntilIdle()

    assertEquals(1, repository.requestCount)
  }

  @Test
  fun `server failure changes connection state`() = runTest(dispatcher) {
    val repository =
      CountingServerStatusRepository(
        result = Result.failure(IllegalStateException("unavailable")),
      )
    val viewModel = MainViewModel(CheckServerStatusUseCase(repository))

    viewModel.checkConnection()
    advanceUntilIdle()

    assertEquals(ServerConnectionUiState.Failed, viewModel.uiState.value.connection)
  }

  @Test
  fun `unavailable server response changes connection state`() = runTest(dispatcher) {
    val repository =
      CountingServerStatusRepository(
        result =
          Result.success(
            ServerStatus(
              code = ServerStatusCode("maintenance"),
              message = ServerStatusMessage("try later"),
            ),
          ),
      )
    val viewModel = MainViewModel(CheckServerStatusUseCase(repository))

    viewModel.checkConnection()
    advanceUntilIdle()

    assertEquals(
      ServerConnectionUiState.Unavailable(status = "maintenance", message = "try later"),
      viewModel.uiState.value.connection,
    )
  }
}

private class CountingServerStatusRepository(
  private val result: Result<ServerStatus> =
    Result.success(
      ServerStatus(
        code = ServerStatusCode("ok"),
        message = ServerStatusMessage("ready"),
      ),
    ),
) : ServerStatusRepository {
  var requestCount: Int = 0
    private set

  override suspend fun getServerStatus(): Result<ServerStatus> {
    requestCount += 1
    return result
  }
}
