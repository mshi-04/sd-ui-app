package com.example.sdui.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sdui.R
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.UiComponentKindUiState
import com.example.sdui.ui.state.UiComponentUiState
import com.example.sdui.ui.state.UiSchemaUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun normalNotLoadedStateShowsPromptAndEnabledButton() {
    // Act
    // Normal: the screen invites the first schema request
    setMainScreen()

    // Assert
    onText(R.string.ui_schema_not_loaded).assertIsDisplayed()
    onText(R.string.load_ui_schema).assertIsEnabled()
  }

  @Test
  fun normalLoadingStateReplacesPromptAndDisablesLoadButton() {
    // Act
    // Normal: the prompt gives way to progress while a request is in flight
    setMainScreen(MainUiState(schema = UiSchemaUiState.Loading))

    // Assert
    onText(R.string.ui_schema_not_loaded).assertDoesNotExist()
    onText(R.string.load_ui_schema).assertIsNotEnabled()
  }

  @Test
  fun interactionLoadButtonInvokesCallback() {
    // Arrange
    var loadCount = 0

    // Act
    // Interaction: tapping the button asks the caller to load the schema
    setMainScreen(onLoadUiSchema = { loadCount += 1 })
    onText(R.string.load_ui_schema).performClick()

    // Assert
    assertEquals(1, loadCount)
  }

  @Test
  fun normalLoadedSchemaShowsNestedComponentLabels() {
    // Arrange
    val card =
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
      )

    // Act
    // Normal: every level of the resolved component tree is rendered
    setMainScreen(MainUiState(schema = loaded(components = listOf(card))))

    // Assert
    composeTestRule.onNodeWithText("Billing").assertIsDisplayed()
    composeTestRule.onNodeWithText("invoices").assertIsDisplayed()
  }

  @Test
  fun boundaryButtonComponentIsRenderedWithoutAnAction() {
    // Arrange
    val button =
      UiComponentUiState(
        id = "refund-action",
        kind = UiComponentKindUiState.BUTTON,
        label = "Refund",
        children = emptyList(),
      )

    // Act
    // Boundary: the contract carries no action, so the rendered control stays inert
    setMainScreen(MainUiState(schema = loaded(components = listOf(button))))

    // Assert
    composeTestRule.onNodeWithText("Refund").assertIsNotEnabled()
  }

  @Test
  fun normalLoadedSchemaShowsServerRevision() {
    // Act
    // Normal: the revision of the resolved schema is visible on the screen
    setMainScreen(MainUiState(schema = loaded()))

    // Assert
    onText(R.string.ui_schema_revision_format, "a1b2c3").assertIsDisplayed()
  }

  @Test
  fun boundaryLoadedSchemaWithoutComponentsShowsEmptyMessage() {
    // Act
    // Boundary: entitlement filtering can leave a surface with nothing to render
    setMainScreen(MainUiState(schema = loaded()))

    // Assert
    onText(R.string.ui_schema_empty).assertIsDisplayed()
  }

  @Test
  fun normalUnauthorizedStateShowsAuthenticationMessage() {
    // Act
    // Normal: a rejected token is reported with a fixed local message
    setMainScreen(MainUiState(schema = UiSchemaUiState.Unauthorized))

    // Assert
    onText(R.string.ui_schema_unauthorized).assertIsDisplayed()
  }

  @Test
  fun normalSurfaceNotFoundStateShowsUnknownSurfaceMessage() {
    // Act
    // Normal: an unknown surface is reported with a fixed local message
    setMainScreen(MainUiState(schema = UiSchemaUiState.SurfaceNotFound))

    // Assert
    onText(R.string.ui_schema_surface_not_found).assertIsDisplayed()
  }

  @Test
  fun normalFailedStateShowsConnectionFailureMessage() {
    // Act
    // Normal: a transport failure is reported with a fixed local message
    setMainScreen(MainUiState(schema = UiSchemaUiState.Failed))

    // Assert
    onText(R.string.ui_schema_failed).assertIsDisplayed()
  }

  private fun loaded(
    components: List<UiComponentUiState> = emptyList(),
  ): UiSchemaUiState.Loaded =
    UiSchemaUiState.Loaded(
      revision = "a1b2c3",
      darkTheme = false,
      compact = false,
      components = components,
    )

  private fun setMainScreen(
    uiState: MainUiState = MainUiState(),
    onLoadUiSchema: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      MainScreen(
        uiState = uiState,
        onLoadUiSchema = onLoadUiSchema,
      )
    }
  }

  private fun onText(resourceId: Int, vararg formatArgs: Any) =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(resourceId, *formatArgs))
}
