package com.example.sdui.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sdui.R
import com.example.sdui.ui.state.LoginUiState
import com.example.sdui.ui.state.SignInUiState
import com.example.sdui.ui.theme.SDUITheme

@Composable
fun LoginScreen(
  uiState: LoginUiState,
  onSignIn: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.headlineSmall,
    )
    Text(
      text = stringResource(R.string.sign_in_description),
      textAlign = TextAlign.Center,
    )
    Button(
      onClick = onSignIn,
      enabled = uiState.signIn !is SignInUiState.InProgress,
    ) {
      Text(stringResource(R.string.sign_in))
    }
    SignInStatus(signIn = uiState.signIn)
  }
}

@Composable
private fun SignInStatus(signIn: SignInUiState) {
  when (signIn) {
    SignInUiState.InProgress -> CircularProgressIndicator()
    SignInUiState.Failed ->
      Text(
        text = stringResource(R.string.sign_in_failed),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
      )
    SignInUiState.Idle, is SignInUiState.Succeeded -> Unit
  }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
  SDUITheme {
    LoginScreen(
      uiState = LoginUiState(),
      onSignIn = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenFailedPreview() {
  SDUITheme {
    LoginScreen(
      uiState = LoginUiState(signIn = SignInUiState.Failed),
      onSignIn = {},
    )
  }
}
