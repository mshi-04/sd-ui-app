package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SignInUseCaseTest {
  @Test
  fun normalInteractiveSignInReturnsTheSignedInSession() = runTest {
    // Arrange
    val session =
      AuthSession.SignedIn(
        AuthAccount(
          id = AccountId("6f2c1d4e-0000-0000-0000-000000000000"),
          name = AccountName("plain@example.onmicrosoft.com"),
        ),
      )
    val repository = mockk<AuthRepository>()
    coEvery { repository.signIn() } returns Result.success(session)
    val useCase = SignInUseCase(repository)

    // Act
    // Normal: the account established by the interactive flow reaches the caller
    val actual = useCase()

    // Assert
    assertEquals(session, actual.getOrThrow())
  }

  @Test
  fun errorCancelledSignInStaysAFailedResult() = runTest {
    // Arrange
    val repository = mockk<AuthRepository>()
    coEvery { repository.signIn() } returns Result.failure(IllegalStateException("cancelled"))
    val useCase = SignInUseCase(repository)

    // Act
    // Error: an abandoned or rejected sign in is reported as a failure, not as a session
    val actual = useCase()

    // Assert
    assertInstanceOf(IllegalStateException::class.java, actual.exceptionOrNull())
  }
}
