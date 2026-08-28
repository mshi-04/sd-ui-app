package com.example.sdui.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.ServerConnectionUiState
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun connectionButtonExists() {
    setContent(MainUiState())

    composeTestRule.onNodeWithText("接続を確認").assertExists()
  }

  @Test
  fun connectedMessageExists() {
    setContent(
      MainUiState(
        connection = ServerConnectionUiState.Connected(status = "ok", message = "ready"),
      ),
    )

    composeTestRule.onNodeWithText("ready").assertExists()
  }

  @Test
  fun unavailableMessageExists() {
    setContent(
      MainUiState(
        connection =
          ServerConnectionUiState.Unavailable(
            status = "maintenance",
            message = "try later",
          ),
      ),
    )

    composeTestRule.onNodeWithText("try later").assertExists()
  }

  @Test
  fun connectionFailureMessageExists() {
    setContent(MainUiState(connection = ServerConnectionUiState.Failed))

    composeTestRule.onNodeWithText("サーバーへ接続できませんでした").assertExists()
  }

  private fun setContent(uiState: MainUiState) {
    composeTestRule.setContent {
      MainScreen(
        uiState = uiState,
        onCheckConnection = {},
      )
    }
  }
}
