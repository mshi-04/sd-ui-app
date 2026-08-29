package com.example.sdui.data.repository

import com.example.sdui.data.model.HealthResponseDto
import com.example.sdui.data.remote.SdUiApi
import com.example.sdui.data.remote.ServerStatusRemoteDataSource
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DefaultServerStatusRepositoryTest {
  @Test
  fun normalRemoteResponseIsMappedToDomainValues() = runTest {
    // Arrange
    val repository = createRepository { HealthResponseDto(status = " ok ", message = " ready ") }

    // Act
    // Normal: the DTO is converted into domain value objects
    val actual = repository.getServerStatus().getOrThrow()

    // Assert
    assertEquals(
      ServerStatus(
        code = ServerStatusCode("ok"),
        message = ServerStatusMessage("ready"),
      ),
      actual,
    )
  }

  @Test
  fun boundaryBlankRemoteMessageIsMappedToNull() = runTest {
    // Arrange
    val repository = createRepository { HealthResponseDto(status = "ok", message = " ") }

    // Act
    // Boundary: a blank message carries no meaning and becomes null
    val actual = repository.getServerStatus().getOrThrow()

    // Assert
    assertNull(actual.message)
  }

  @Test
  fun errorRemoteFailureBecomesFailedResult() = runTest {
    // Arrange
    val repository = createRepository { throw IOException("offline") }

    // Act
    // Error: a transport failure is folded into a failed result instead of escaping
    val actual = repository.getServerStatus()

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun errorInvalidStatusBecomesFailedResult() = runTest {
    // Arrange
    val repository = createRepository { HealthResponseDto(status = " ", message = "ready") }

    // Act
    // Error: a value object constraint violation is reported as a failed result
    val actual = repository.getServerStatus()

    // Assert
    assertInstanceOf(IllegalArgumentException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun coroutineCancellationIsRethrownInsteadOfBeingFolded() = runTest {
    // Arrange
    val repository = createRepository { throw CancellationException("cancelled") }

    // Act
    // Coroutine: cancellation is rethrown so the caller is not kept alive by a failed result
    val thrown = runCatching { repository.getServerStatus() }.exceptionOrNull()

    // Assert
    assertInstanceOf(CancellationException::class.java, thrown)
  }

  private fun createRepository(response: () -> HealthResponseDto): DefaultServerStatusRepository =
    DefaultServerStatusRepository(ServerStatusRemoteDataSource(FakeSdUiApi(response)))
}

private class FakeSdUiApi(
  private val response: () -> HealthResponseDto,
) : SdUiApi {
  override suspend fun getHealth(): HealthResponseDto = response()
}
