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
import com.example.sdui.ui.state.ServerConnectionUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun normalNotCheckedStateShowsPromptAndEnabledButton() {
    // Act
    // Normal: the screen invites the first connection check
    setMainScreen()

    // Assert
    onText(R.string.connection_not_checked).assertIsDisplayed()
    onText(R.string.check_connection).assertIsEnabled()
  }

  @Test
  fun normalCheckingStateReplacesPromptAndDisablesCheckButton() {
    // Act
    // Normal: the prompt gives way to progress while a request is in flight
    setMainScreen(MainUiState(connection = ServerConnectionUiState.Checking))

    // Assert
    onText(R.string.connection_not_checked).assertDoesNotExist()
    onText(R.string.check_connection).assertIsNotEnabled()
  }

  @Test
  fun interactionCheckButtonInvokesCallback() {
    // Arrange
    var checkCount = 0

    // Act
    // Interaction: tapping the button asks the caller to run a check
    setMainScreen(onCheckConnection = { checkCount += 1 })
    onText(R.string.check_connection).performClick()

    // Assert
    assertEquals(1, checkCount)
  }

  @Test
  fun normalConnectedStateShowsServerMessage() {
    // Arrange
    val connection = ServerConnectionUiState.Connected(status = "ok", message = "ready")

    // Act
    // Normal: the server message is preferred for an available server
    setMainScreen(MainUiState(connection = connection))

    // Assert
    composeTestRule.onNodeWithText("ready").assertIsDisplayed()
  }

  @Test
  fun boundaryConnectedStateWithoutMessageShowsStatusFallback() {
    // Arrange
    val connection = ServerConnectionUiState.Connected(status = "ok", message = null)

    // Act
    // Boundary: without a server message the status code is shown instead
    setMainScreen(MainUiState(connection = connection))

    // Assert
    onText(R.string.connection_status_format, "ok").assertIsDisplayed()
  }

  @Test
  fun normalUnavailableStateShowsServerMessage() {
    // Arrange
    val connection =
      ServerConnectionUiState.Unavailable(status = "maintenance", message = "try later")

    // Act
    // Normal: the server message is preferred for an unavailable server
    setMainScreen(MainUiState(connection = connection))

    // Assert
    composeTestRule.onNodeWithText("try later").assertIsDisplayed()
  }

  @Test
  fun boundaryUnavailableStateWithoutMessageShowsStatusFallback() {
    // Arrange
    val connection = ServerConnectionUiState.Unavailable(status = "maintenance", message = null)

    // Act
    // Boundary: without a server message the status code is shown instead
    setMainScreen(MainUiState(connection = connection))

    // Assert
    onText(R.string.connection_unavailable_format, "maintenance").assertIsDisplayed()
  }

  @Test
  fun normalFailedStateShowsConnectionFailureMessage() {
    // Act
    // Normal: a transport failure is reported with a fixed local message
    setMainScreen(MainUiState(connection = ServerConnectionUiState.Failed))

    // Assert
    onText(R.string.connection_failed).assertIsDisplayed()
  }

  private fun setMainScreen(
    uiState: MainUiState = MainUiState(),
    onCheckConnection: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      MainScreen(
        uiState = uiState,
        onCheckConnection = onCheckConnection,
      )
    }
  }

  private fun onText(resourceId: Int, vararg formatArgs: Any) =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(resourceId, *formatArgs))
}
