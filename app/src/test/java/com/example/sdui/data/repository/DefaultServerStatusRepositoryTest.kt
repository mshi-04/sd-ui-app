package com.example.sdui.data.repository

import com.example.sdui.data.model.HealthResponseDto
import com.example.sdui.data.remote.SdUiApi
import com.example.sdui.data.remote.ServerStatusRemoteDataSource
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultServerStatusRepositoryTest {
  @Test
  fun `remote response is mapped to domain values`() = runTest {
    val repository =
      DefaultServerStatusRepository(
        ServerStatusRemoteDataSource(
          FakeSdUiApi(HealthResponseDto(status = " ok ", message = " ready ")),
        ),
      )

    val actual = repository.getServerStatus().getOrThrow()

    assertEquals(
      ServerStatus(
        code = ServerStatusCode("ok"),
        message = ServerStatusMessage("ready"),
      ),
      actual,
    )
  }

  @Test
  fun `blank remote message is mapped to null`() = runTest {
    val repository =
      DefaultServerStatusRepository(
        ServerStatusRemoteDataSource(
          FakeSdUiApi(HealthResponseDto(status = "ok", message = " ")),
        ),
      )

    val actual = repository.getServerStatus().getOrThrow()

    assertEquals(null, actual.message)
  }
}

private class FakeSdUiApi(
  private val response: HealthResponseDto,
) : SdUiApi {
  override suspend fun getHealth(): HealthResponseDto = response
}
