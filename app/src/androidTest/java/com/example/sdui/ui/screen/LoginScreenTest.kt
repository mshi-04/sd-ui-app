package com.example.sdui.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sdui.R
import com.example.sdui.ui.state.LoginUiState
import com.example.sdui.ui.state.SignInUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun normalIdleStateInvitesSignIn() {
    // Act
    // Normal: the screen explains why signing in is required and offers the button
    setLoginScreen()

    // Assert
    onText(R.string.sign_in_description).assertIsDisplayed()
    onText(R.string.sign_in).assertIsEnabled()
  }

  @Test
  fun normalInProgressStateDisablesSignInButton() {
    // Act
    // Normal: the interactive flow owns the screen while it runs
    setLoginScreen(LoginUiState(signIn = SignInUiState.InProgress))

    // Assert
    onText(R.string.sign_in).assertIsNotEnabled()
  }

  @Test
  fun normalFailedStateShowsFailureMessageAndAllowsRetry() {
    // Act
    // Normal: a failed sign in is reported and the button stays usable
    setLoginScreen(LoginUiState(signIn = SignInUiState.Failed))

    // Assert
    onText(R.string.sign_in_failed).assertIsDisplayed()
    onText(R.string.sign_in).assertIsEnabled()
  }

  @Test
  fun interactionSignInButtonInvokesCallback() {
    // Arrange
    var signInCount = 0

    // Act
    // Interaction: tapping the button asks the caller to start the interactive flow
    setLoginScreen(onSignIn = { signInCount += 1 })
    onText(R.string.sign_in).performClick()

    // Assert
    assertEquals(1, signInCount)
  }

  private fun setLoginScreen(
    uiState: LoginUiState = LoginUiState(),
    onSignIn: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      LoginScreen(
        uiState = uiState,
        onSignIn = onSignIn,
      )
    }
  }

  private fun onText(resourceId: Int, vararg formatArgs: Any) =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(resourceId, *formatArgs))
}
