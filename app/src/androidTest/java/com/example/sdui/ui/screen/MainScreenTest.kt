package com.example.sdui.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sdui.R
import com.example.sdui.ui.model.DashboardUiKind
import com.example.sdui.ui.model.DashboardUiModel
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.MainUiStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun normalLoadingStateDisablesTheReloadButton() {
    // Act
    // Normal: the screen loads on its own, so reloading is blocked while a request is in flight
    setMainScreen(MainUiState(status = MainUiStatus.LOADING))

    // Assert
    onText(R.string.load_ui_schema).assertIsNotEnabled()
  }

  @Test
  fun normalSettledStateAllowsReloading() {
    // Act
    // Normal: once a request has settled the schema can be requested again
    setMainScreen()

    // Assert
    onText(R.string.load_ui_schema).assertIsEnabled()
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
      DashboardUiModel(
        id = "billing-summary",
        kind = DashboardUiKind.CARD,
        label = "Billing",
        children =
          listOf(
            DashboardUiModel(
              id = "invoice-list",
              kind = DashboardUiKind.LIST,
              label = "invoices",
              children = emptyList(),
            ),
          ),
      )

    // Act
    // Normal: every level of the resolved component tree is rendered
    setMainScreen(loaded(components = listOf(card)))

    // Assert
    composeTestRule.onNodeWithText("Billing").assertIsDisplayed()
    composeTestRule.onNodeWithText("invoices").assertIsDisplayed()
  }

  @Test
  fun boundaryButtonComponentIsRenderedWithoutAnAction() {
    // Arrange
    val button =
      DashboardUiModel(
        id = "refund-action",
        kind = DashboardUiKind.BUTTON,
        label = "Refund",
        children = emptyList(),
      )

    // Act
    // Boundary: the contract carries no action, so the rendered control stays inert
    setMainScreen(loaded(components = listOf(button)))

    // Assert
    composeTestRule.onNodeWithText("Refund").assertIsNotEnabled()
  }

  @Test
  fun normalSignedInAccountIsShownOnTheScreen() {
    // Act
    // Normal: the signed in account is visible so the resolved schema can be attributed to it
    setMainScreen()

    // Assert
    onText(R.string.signed_in_as_format, ACCOUNT_NAME).assertIsDisplayed()
  }

  @Test
  fun interactionSignOutButtonInvokesCallback() {
    // Arrange
    var signOutCount = 0

    // Act
    // Interaction: tapping the button asks the caller to drop the session
    setMainScreen(onSignOut = { signOutCount += 1 })
    onText(R.string.sign_out).performClick()

    // Assert
    assertEquals(1, signOutCount)
  }

  @Test
  fun normalLoadedSchemaShowsServerRevision() {
    // Act
    // Normal: the revision of the resolved schema is visible on the screen
    setMainScreen(loaded())

    // Assert
    onText(R.string.ui_schema_revision_format, "a1b2c3").assertIsDisplayed()
  }

  @Test
  fun boundaryLoadedSchemaWithoutComponentsShowsEmptyMessage() {
    // Act
    // Boundary: entitlement filtering can leave a surface with nothing to render
    setMainScreen(loaded())

    // Assert
    onText(R.string.ui_schema_empty).assertIsDisplayed()
  }

  @Test
  fun normalUnauthorizedStateShowsAuthenticationMessage() {
    // Act
    // Normal: a rejected token is reported with a fixed local message
    setMainScreen(MainUiState(status = MainUiStatus.UNAUTHORIZED))

    // Assert
    onText(R.string.ui_schema_unauthorized).assertIsDisplayed()
  }

  @Test
  fun normalSurfaceNotFoundStateShowsUnknownSurfaceMessage() {
    // Act
    // Normal: an unknown surface is reported with a fixed local message
    setMainScreen(MainUiState(status = MainUiStatus.SURFACE_NOT_FOUND))

    // Assert
    onText(R.string.ui_schema_surface_not_found).assertIsDisplayed()
  }

  @Test
  fun normalUnsuccessfulRequestShowsTheConfiguredBaseUrl() {
    // Arrange
    val uiState =
      MainUiState(apiBaseUrl = API_BASE_URL, status = MainUiStatus.SURFACE_NOT_FOUND)

    // Act
    // Normal: a wrong host has to be distinguishable from a server side failure
    setMainScreen(uiState)

    // Assert
    onText(R.string.api_base_url_format, API_BASE_URL).assertIsDisplayed()
  }

  @Test
  fun boundarySuccessfulRequestKeepsTheBaseUrlOffTheScreen() {
    // Arrange
    val uiState = loaded().copy(apiBaseUrl = API_BASE_URL)

    // Act
    // Boundary: the diagnostic caption only belongs on the states that could be a misconfiguration
    setMainScreen(uiState)

    // Assert
    onText(R.string.api_base_url_format, API_BASE_URL).assertDoesNotExist()
  }

  @Test
  fun normalFailedStateShowsConnectionFailureMessage() {
    // Act
    // Normal: a transport failure is reported with a fixed local message
    setMainScreen(MainUiState(status = MainUiStatus.FAILED))

    // Assert
    onText(R.string.ui_schema_failed).assertIsDisplayed()
  }

  private fun loaded(
    components: List<DashboardUiModel> = emptyList(),
  ): MainUiState =
    MainUiState(
      status = MainUiStatus.LOADED,
      revision = "a1b2c3",
      darkTheme = false,
      compact = false,
      components = components,
    )

  private fun setMainScreen(
    uiState: MainUiState = loaded(),
    accountName: String = ACCOUNT_NAME,
    onLoadUiSchema: () -> Unit = {},
    onSignOut: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      MainScreen(
        uiState = uiState,
        accountName = accountName,
        onLoadUiSchema = onLoadUiSchema,
        onSignOut = onSignOut,
      )
    }
  }

  private fun onText(resourceId: Int, vararg formatArgs: Any) =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(resourceId, *formatArgs))

  private companion object {
    const val ACCOUNT_NAME = "plain@example.onmicrosoft.com"
    const val API_BASE_URL = "https://sdui-api.example.invalid/api/"
  }
}
