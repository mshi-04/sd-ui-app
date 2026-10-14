package com.example.sdui.data.repository

import com.example.sdui.data.auth.MsalAccount
import com.example.sdui.data.auth.MsalAuthClient
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DefaultAuthRepositoryTest {
  private val client = mockk<MsalAuthClient>()
  private val repository = DefaultAuthRepository(client)

  @ParameterizedTest
  @EnumSource(Operation::class)
  fun interactionOperationCallsOnlyItsCorrespondingAdapterMethod(operation: Operation) = runTest {
    // Arrange
    coEvery { client.signIn() } returns null
    coEvery { client.signOut() } returns Unit

    // Act
    // Interaction: each repository operation delegates only to the matching adapter operation
    operation.execute(repository)

    // Assert
    assertAll(
      { coVerify(exactly = if (operation == Operation.SignIn) 1 else 0) { client.signIn() } },
      { coVerify(exactly = if (operation == Operation.SignOut) 1 else 0) { client.signOut() } },
    )
  }

  @Test
  fun normalSignInAccountIsMappedToNormalizedSignedInSession() = runTest {
    // Arrange
    coEvery { client.signIn() } returns MsalAccount(" account-id ", " user@example.com ")

    // Act
    // Normal: account data is normalized through domain value objects
    val actual = repository.signIn().getOrThrow()

    // Assert
    assertEquals(
      AuthSession.SignedIn(AuthAccount(AccountId("account-id"), AccountName("user@example.com"))),
      actual,
    )
  }

  @Test
  fun boundarySignInMissingAccountBecomesSignedOutSession() = runTest {
    // Arrange
    coEvery { client.signIn() } returns null

    // Act
    // Boundary: the adapter's missing account maps to the signed-out domain session
    val actual = repository.signIn().getOrThrow()

    // Assert
    assertEquals(AuthSession.SignedOut, actual)
  }

  @Test
  fun errorSignInInvalidAccountIdBecomesFailedResult() = runTest {
    // Arrange
    coEvery { client.signIn() } returns MsalAccount(" ", "user@example.com")

    // Act
    // Error: invalid adapter data cannot create a signed-in domain session
    val actual = repository.signIn()

    // Assert
    assertInstanceOf(IllegalArgumentException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun errorSignInInvalidAccountNameBecomesFailedResult() = runTest {
    // Arrange
    coEvery { client.signIn() } returns MsalAccount("account-id", " ")

    // Act
    // Error: blank account names are rejected during domain conversion
    val actual = repository.signIn()

    // Assert
    assertInstanceOf(IllegalArgumentException::class.java, actual.exceptionOrNull())
  }

  @ParameterizedTest
  @EnumSource(Operation::class)
  fun errorAdapterExceptionBecomesFailedResult(operation: Operation) = runTest {
    // Arrange
    val error = IOException("authentication unavailable")
    coEvery { client.signIn() } throws error
    coEvery { client.signOut() } throws error

    // Act
    // Error: each operation folds adapter failures into Result without replacing the cause
    val actual = operation.execute(repository)

    // Assert
    assertSame(error, actual.exceptionOrNull())
  }

  @ParameterizedTest
  @EnumSource(Operation::class)
  fun coroutineCancellationIsRethrownForEachOperation(operation: Operation) = runTest {
    // Arrange
    val cancellation = CancellationException("cancelled")
    coEvery { client.signIn() } throws cancellation
    coEvery { client.signOut() } throws cancellation

    // Act
    // Coroutine: every repository operation preserves coroutine cancellation
    val actual = runCatching { operation.execute(repository) }.exceptionOrNull()

    // Assert
    assertSame(cancellation, actual)
  }

  @Test
  fun normalSignOutReturnsSuccessfulUnit() = runTest {
    // Arrange
    coEvery { client.signOut() } returns Unit

    // Act
    // Normal: completed account removal produces a successful Unit result
    val actual = repository.signOut()

    // Assert
    assertAll(
      { assertEquals(Unit, actual.getOrThrow()) },
      { coVerify(exactly = 1) { client.signOut() } },
    )
  }

  enum class Operation {
    SignIn,
    SignOut;

    suspend fun execute(repository: DefaultAuthRepository): Result<*> = when (this) {
      SignIn -> repository.signIn()
      SignOut -> repository.signOut()
    }
  }
}
