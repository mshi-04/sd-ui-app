package com.example.sdui.domain.usecase

import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.repository.UiSchemaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SignOutUseCaseTest {
  private val authRepository = mockk<AuthRepository>()
  private val uiSchemaRepository = mockk<UiSchemaRepository>()

  @Test
  fun interactionSignOutIsDelegatedToTheRepository() = runTest {
    // Arrange
    coEvery { authRepository.signOut() } returns Result.success(Unit)
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)

    // Act
    // Interaction: the cached account is dropped through the repository boundary
    useCase()

    // Assert
    coVerify(exactly = 1) { authRepository.signOut() }
  }

  @Test
  fun interactionSchemaCacheIsClearedAfterSuccessfulSignOut() = runTest {
    // Arrange
    coEvery { authRepository.signOut() } returns Result.success(Unit)
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)

    // Act
    // Interaction: the previous account's schema is discarded on sign-out
    useCase()

    // Assert
    verify(exactly = 1) { uiSchemaRepository.clearSchema() }
  }

  @Test
  fun errorRepositoryFailureStaysAFailedResult() = runTest {
    // Arrange
    coEvery { authRepository.signOut() } returns Result.failure(IOException("offline"))
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)

    // Act
    // Error: a failure is not swallowed here, so the caller can still distinguish it
    val actual = useCase()

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun errorSchemaCacheIsClearedEvenWhenSignOutFails() = runTest {
    // Arrange
    coEvery { authRepository.signOut() } returns Result.failure(IOException("offline"))
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)

    // Act
    // Error: a failed sign-out must not leave the previous account's schema behind
    useCase()

    // Assert
    verify(exactly = 1) { uiSchemaRepository.clearSchema() }
  }

  @Test
  fun interactionSchemaCacheIsClearedAfterAuthSignOut() = runTest {
    // Arrange
    coEvery { authRepository.signOut() } returns Result.success(Unit)
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)

    // Act
    // Interaction: clearing runs after the auth sign-out, not before
    useCase()

    // Assert
    coVerifyOrder {
      authRepository.signOut()
      uiSchemaRepository.clearSchema()
    }
  }

  @Test
  fun coroutineSchemaCacheIsClearedWhenSignOutIsCancelled() = runTest {
    // Arrange
    val started = CompletableDeferred<Unit>()
    coEvery { authRepository.signOut() } coAnswers {
      started.complete(Unit)
      CompletableDeferred<Result<Unit>>().await()
    }
    every { uiSchemaRepository.clearSchema() } returns Unit
    val useCase = SignOutUseCase(authRepository, uiSchemaRepository)
    val job = launch(start = CoroutineStart.UNDISPATCHED) { useCase() }
    started.await()

    // Act
    // Coroutine: cancellation during sign-out still discards the cached schema
    job.cancel()
    job.join()

    // Assert
    verify(exactly = 1) { uiSchemaRepository.clearSchema() }
  }
}
