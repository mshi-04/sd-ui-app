package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.repository.UiSchemaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class LoadUiSchemaUseCaseTest {
  @Test
  fun normalResolvedSchemaIsReturnedToTheCaller() = runTest {
    // Arrange
    val resolution = UiSchemaResolution.Resolved(schema())
    val useCase = LoadUiSchemaUseCase(FakeUiSchemaRepository(Result.success(resolution)))

    // Act
    // Normal: the resolved schema reaches the caller unchanged
    val actual = useCase()

    // Assert
    assertEquals(resolution, actual.getOrThrow())
  }

  @Test
  fun normalContractOutcomeIsReturnedToTheCaller() = runTest {
    // Arrange
    val repository = FakeUiSchemaRepository(Result.success(UiSchemaResolution.Unauthorized))
    val useCase = LoadUiSchemaUseCase(repository)

    // Act
    // Normal: an outcome written in the contract is passed through as a successful result
    val actual = useCase()

    // Assert
    assertEquals(UiSchemaResolution.Unauthorized, actual.getOrThrow())
  }

  @Test
  fun errorRepositoryFailureStaysAFailedResult() = runTest {
    // Arrange
    val failure = Result.failure<UiSchemaResolution>(IOException("offline"))
    val useCase = LoadUiSchemaUseCase(FakeUiSchemaRepository(failure))

    // Act
    // Error: a failure is not swallowed here, so the caller can still distinguish it
    val actual = useCase()

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun interactionCallWithoutSurfaceAsksForTheDashboardSurface() = runTest {
    // Arrange
    val repository = mockRepository()
    val useCase = LoadUiSchemaUseCase(repository)

    // Act
    // Interaction: the app has a single surface, so the caller does not have to name it
    useCase()

    // Assert
    coVerify(exactly = 1) { repository.getUiSchema(SurfaceId.DASHBOARD) }
  }

  @Test
  fun interactionExplicitSurfaceIsPassedToTheRepository() = runTest {
    // Arrange
    val repository = mockRepository()
    val useCase = LoadUiSchemaUseCase(repository)

    // Act
    // Interaction: a named surface overrides the default without another entry point
    useCase(SurfaceId("settings"))

    // Assert
    coVerify(exactly = 1) { repository.getUiSchema(SurfaceId("settings")) }
  }

  private fun mockRepository(): UiSchemaRepository {
    val repository = mockk<UiSchemaRepository>()
    coEvery { repository.getUiSchema(any()) } returns
      Result.success(UiSchemaResolution.Resolved(schema()))
    return repository
  }

  private fun schema(): UiSchema =
    UiSchema(
      surface = SurfaceId.DASHBOARD,
      version = "2026-06-01",
      revision = "a1b2c3",
      theme = UiTheme.LIGHT,
      density = UiDensity.COMFORTABLE,
      components = emptyList(),
    )
}

private class FakeUiSchemaRepository(
  private val result: Result<UiSchemaResolution>,
) : UiSchemaRepository {
  override suspend fun getUiSchema(surface: SurfaceId): Result<UiSchemaResolution> = result
}
