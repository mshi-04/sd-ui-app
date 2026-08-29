package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.ServerAvailability
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import com.example.sdui.domain.repository.ServerStatusRepository
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CheckServerStatusUseCaseTest {
  @Test
  fun normalOkStatusIsInterpretedAsAvailable() = runTest {
    // Arrange
    val status = serverStatus(code = "ok")
    val useCase = createUseCase(Result.success(status))

    // Act
    // Normal: an "ok" status is interpreted as available
    val actual = useCase().getOrThrow()

    // Assert
    assertEquals(ServerAvailability.Available(status), actual)
  }

  @Test
  fun boundaryUpperCaseOkStatusIsInterpretedAsAvailable() = runTest {
    // Arrange
    val status = serverStatus(code = "OK")
    val useCase = createUseCase(Result.success(status))

    // Act
    // Boundary: the status is compared without regard to case
    val actual = useCase().getOrThrow()

    // Assert
    assertEquals(ServerAvailability.Available(status), actual)
  }

  @Test
  fun normalNonOkStatusIsInterpretedAsUnavailable() = runTest {
    // Arrange
    val status = serverStatus(code = "maintenance", message = "try later")
    val useCase = createUseCase(Result.success(status))

    // Act
    // Normal: any status other than "ok" is interpreted as unavailable
    val actual = useCase().getOrThrow()

    // Assert
    assertEquals(ServerAvailability.Unavailable(status), actual)
  }

  @Test
  fun errorRepositoryFailureIsPropagated() = runTest {
    // Arrange
    val failure = IOException("offline")
    val useCase = createUseCase(Result.failure(failure))

    // Act
    // Error: a failed request is passed through without being interpreted
    val actual = useCase()

    // Assert
    assertSame(failure, actual.exceptionOrNull())
  }

  private fun createUseCase(result: Result<ServerStatus>): CheckServerStatusUseCase =
    CheckServerStatusUseCase(FakeServerStatusRepository(result))

  private fun serverStatus(code: String, message: String = "ready"): ServerStatus =
    ServerStatus(
      code = ServerStatusCode(code),
      message = ServerStatusMessage(message),
    )
}

private class FakeServerStatusRepository(
  private val result: Result<ServerStatus>,
) : ServerStatusRepository {
  override suspend fun getServerStatus(): Result<ServerStatus> = result
}
