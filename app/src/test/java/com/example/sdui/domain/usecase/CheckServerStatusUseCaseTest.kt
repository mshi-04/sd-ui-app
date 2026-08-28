package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.ServerAvailability
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import com.example.sdui.domain.repository.ServerStatusRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckServerStatusUseCaseTest {
  @Test
  fun `repository result is returned`() = runTest {
    val expected =
      ServerStatus(
        code = ServerStatusCode("ok"),
        message = ServerStatusMessage("ready"),
      )
    val useCase = CheckServerStatusUseCase(FakeServerStatusRepository(Result.success(expected)))

    val actual = useCase().getOrThrow()

    assertEquals(ServerAvailability.Available(expected), actual)
  }

  @Test
  fun `non ok status is unavailable`() = runTest {
    val status =
      ServerStatus(
        code = ServerStatusCode("maintenance"),
        message = ServerStatusMessage("try later"),
      )
    val useCase = CheckServerStatusUseCase(FakeServerStatusRepository(Result.success(status)))

    val actual = useCase().getOrThrow()

    assertEquals(ServerAvailability.Unavailable(status), actual)
  }
}

private class FakeServerStatusRepository(
  private val result: Result<ServerStatus>,
) : ServerStatusRepository {
  override suspend fun getServerStatus(): Result<ServerStatus> = result
}
