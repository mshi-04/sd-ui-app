package com.example.sdui.ui.viewmodel

import app.cash.turbine.test
import com.example.sdui.domain.model.UiComponent
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.model.value.UiComponentId
import com.example.sdui.domain.repository.UiSchemaRepository
import com.example.sdui.domain.usecase.LoadUiSchemaUseCase
import com.example.sdui.ui.state.UiComponentKindUiState
import com.example.sdui.ui.state.UiComponentUiState
import com.example.sdui.ui.state.UiSchemaUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun normalInitialStateIsNotLoaded() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(resolved())

    // Act
    // Normal: no request is made until the screen asks for one
    val actual = viewModel.uiState.value.schema

    // Assert
    assertEquals(UiSchemaUiState.NotLoaded, actual)
  }

  @Test
  fun flowLoadUiSchemaEmitsLoadingBeforeLoaded() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(resolved(components = emptyList()))

    // Act & Assert
    // Flow: the UI state passes through loading before reaching the loaded schema
    viewModel.uiState.test {
      assertEquals(UiSchemaUiState.NotLoaded, awaitItem().schema)
      viewModel.loadUiSchema()
      advanceUntilIdle()
      assertEquals(UiSchemaUiState.Loading, awaitItem().schema)
      assertEquals(
        UiSchemaUiState.Loaded(
          revision = "a1b2c3",
          darkTheme = false,
          compact = false,
          components = emptyList(),
        ),
        awaitItem().schema,
      )
    }
  }

  @Test
  fun normalResolvedComponentsKeepTheirNestingInUiState() = runTest(dispatcher) {
    // Arrange
    val card =
      component(
        id = "billing-summary",
        type = UiComponentType.CARD,
        props = mapOf("title" to "Billing"),
        children =
          listOf(
            component(
              id = "invoice-list",
              type = UiComponentType.LIST,
              props = mapOf("source" to "invoices"),
            ),
          ),
      )
    val viewModel = createViewModel(resolved(components = listOf(card)))

    // Act
    // Normal: the component tree is flattened into UI state without losing its structure
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals(
      listOf(
        UiComponentUiState(
          id = "billing-summary",
          kind = UiComponentKindUiState.CARD,
          label = "Billing",
          children =
            listOf(
              UiComponentUiState(
                id = "invoice-list",
                kind = UiComponentKindUiState.LIST,
                label = "invoices",
                children = emptyList(),
              ),
            ),
        ),
      ),
      loadedSchema(viewModel).components,
    )
  }

  @Test
  fun normalEachComponentKindTakesItsLabelFromItsOwnProp() = runTest(dispatcher) {
    // Arrange
    val components =
      listOf(
        component(id = "card", type = UiComponentType.CARD, props = mapOf("title" to "Billing")),
        component(id = "list", type = UiComponentType.LIST, props = mapOf("source" to "invoices")),
        component(id = "chart", type = UiComponentType.CHART, props = mapOf("metric" to "requests")),
        component(id = "button", type = UiComponentType.BUTTON, props = mapOf("label" to "Refund")),
        component(id = "banner", type = UiComponentType.BANNER, props = mapOf("tone" to "warning")),
        component(
          id = "panel",
          type = UiComponentType.PANEL,
          props = mapOf("source" to "ui_definitions"),
        ),
      )
    val viewModel = createViewModel(resolved(components = components))

    // Act
    // Normal: the prop that carries the display text differs per component type
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals(
      listOf(
        UiComponentKindUiState.CARD to "Billing",
        UiComponentKindUiState.LIST to "invoices",
        UiComponentKindUiState.CHART to "requests",
        UiComponentKindUiState.BUTTON to "Refund",
        UiComponentKindUiState.BANNER to "warning",
        UiComponentKindUiState.PANEL to "ui_definitions",
      ),
      loadedSchema(viewModel).components.map { component -> component.kind to component.label },
    )
  }

  @Test
  fun boundaryComponentWithoutItsLabelPropFallsBackToTheComponentId() = runTest(dispatcher) {
    // Arrange
    val components = listOf(component(id = "usage-summary", type = UiComponentType.CARD))
    val viewModel = createViewModel(resolved(components = components))

    // Act
    // Boundary: props are free-form, so a missing display prop still leaves the component named
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals("usage-summary", loadedSchema(viewModel).components.single().label)
  }

  @Test
  fun normalPresentationHintsAreExposedAsScreenFlags() = runTest(dispatcher) {
    // Arrange
    val resolution = resolved(theme = UiTheme.DARK, density = UiDensity.COMPACT)
    val viewModel = createViewModel(resolution)

    // Act
    // Normal: theme and density are reduced to the flags the screen applies
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    val actual = loadedSchema(viewModel)
    assertAll(
      { assertEquals(true, actual.darkTheme) },
      { assertEquals(true, actual.compact) },
    )
  }

  @Test
  fun stateTransitionUnauthorizedResolutionExposesUnauthorizedState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(UiSchemaResolution.Unauthorized)

    // Act
    // StateTransition: a rejected token is shown as its own state instead of a failure
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals(UiSchemaUiState.Unauthorized, viewModel.uiState.value.schema)
  }

  @Test
  fun stateTransitionSurfaceNotFoundResolutionExposesSurfaceNotFoundState() = runTest(dispatcher) {
    // Arrange
    val viewModel = createViewModel(UiSchemaResolution.SurfaceNotFound)

    // Act
    // StateTransition: an unknown surface is shown as its own state instead of a failure
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals(UiSchemaUiState.SurfaceNotFound, viewModel.uiState.value.schema)
  }

  @Test
  fun stateTransitionRepositoryFailureExposesFailedState() = runTest(dispatcher) {
    // Arrange
    val failure = Result.failure<UiSchemaResolution>(IOException("offline"))
    val viewModel = createViewModel(FakeUiSchemaRepository(failure))

    // Act
    // StateTransition: a failed request is exposed as the failed state
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    assertEquals(UiSchemaUiState.Failed, viewModel.uiState.value.schema)
  }

  @Test
  fun interactionLoadWhileLoadingSkipsDuplicateRequest() = runTest(dispatcher) {
    // Arrange
    val repository = mockRepositoryReturning(resolved())
    val viewModel = createViewModel(repository)

    // Act
    // Interaction: a request started while loading does not reach the repository twice
    viewModel.loadUiSchema()
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 1) { repository.getUiSchema(SurfaceId.DASHBOARD) }
  }

  @Test
  fun interactionLoadAfterCompletionRequestsAgain() = runTest(dispatcher) {
    // Arrange
    val repository = mockRepositoryReturning(resolved())
    val viewModel = createViewModel(repository)

    // Act
    // Interaction: the guard only blocks while loading, so a finished load can run again
    viewModel.loadUiSchema()
    advanceUntilIdle()
    viewModel.loadUiSchema()
    advanceUntilIdle()

    // Assert
    coVerify(exactly = 2) { repository.getUiSchema(SurfaceId.DASHBOARD) }
  }

  private fun createViewModel(resolution: UiSchemaResolution): MainViewModel =
    createViewModel(FakeUiSchemaRepository(Result.success(resolution)))

  private fun createViewModel(repository: UiSchemaRepository): MainViewModel =
    MainViewModel(LoadUiSchemaUseCase(repository))

  private fun mockRepositoryReturning(resolution: UiSchemaResolution): UiSchemaRepository {
    val repository = mockk<UiSchemaRepository>()
    coEvery { repository.getUiSchema(SurfaceId.DASHBOARD) } returns Result.success(resolution)
    return repository
  }

  private fun loadedSchema(viewModel: MainViewModel): UiSchemaUiState.Loaded =
    viewModel.uiState.value.schema as UiSchemaUiState.Loaded

  private fun resolved(
    theme: UiTheme = UiTheme.LIGHT,
    density: UiDensity = UiDensity.COMFORTABLE,
    components: List<UiComponent> = emptyList(),
  ): UiSchemaResolution.Resolved =
    UiSchemaResolution.Resolved(
      UiSchema(
        surface = SurfaceId.DASHBOARD,
        version = "2026-06-01",
        revision = "a1b2c3",
        theme = theme,
        density = density,
        components = components,
      ),
    )

  private fun component(
    id: String,
    type: UiComponentType,
    props: Map<String, String> = emptyMap(),
    children: List<UiComponent> = emptyList(),
  ): UiComponent =
    UiComponent(
      id = UiComponentId(id),
      type = type,
      props = props,
      children = children,
    )
}

private class FakeUiSchemaRepository(
  private val result: Result<UiSchemaResolution>,
) : UiSchemaRepository {
  override suspend fun getUiSchema(surface: SurfaceId): Result<UiSchemaResolution> = result
}
