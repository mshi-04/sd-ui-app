package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class RestoreSessionUseCaseTest {
  @Test
  fun normalCachedSessionIsReturnedToTheCaller() = runTest {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.restoreSession() } returns Result.success(AuthSession.SignedOut)
    val useCase = RestoreSessionUseCase(repository)

    // Act
    // Normal: the silently restored session reaches the caller unchanged
    val actual = useCase()

    // Assert
    assertEquals(AuthSession.SignedOut, actual.getOrThrow())
  }

  @Test
  fun errorRepositoryFailureStaysAFailedResult() = runTest {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.restoreSession() } returns Result.failure(IOException("offline"))
    val useCase = RestoreSessionUseCase(repository)

    // Act
    // Error: a failure is not swallowed here, so the caller can still distinguish it
    val actual = useCase()

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }
}
