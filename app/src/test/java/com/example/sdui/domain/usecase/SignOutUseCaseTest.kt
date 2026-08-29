package com.example.sdui.domain.usecase

import com.example.sdui.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SignOutUseCaseTest {
  @Test
  fun interactionSignOutIsDelegatedToTheRepository() = runTest {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.signOut() } returns Result.success(Unit)
    val useCase = SignOutUseCase(repository)

    // Act
    // Interaction: the cached account is dropped through the repository boundary
    useCase()

    // Assert
    coVerify(exactly = 1) { repository.signOut() }
  }

  @Test
  fun errorRepositoryFailureStaysAFailedResult() = runTest {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.signOut() } returns Result.failure(IOException("offline"))
    val useCase = SignOutUseCase(repository)

    // Act
    // Error: a failure is not swallowed here, so the caller can still distinguish it
    val actual = useCase()

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }
}
