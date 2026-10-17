package com.example.sdui.ui.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.ui.screen.LoginScreen
import com.example.sdui.ui.state.LoginUiStatus
import com.example.sdui.ui.viewmodel.LoginViewModel

@Composable
fun LoginRoute(
  onSignedIn: (String) -> Boolean,
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(uiState) {
    if (uiState.status != LoginUiStatus.SUCCEEDED) return@LaunchedEffect
    if (onSignedIn(uiState.accountName)) viewModel.onSignedInHandled()
  }

  LoginScreen(
    uiState = uiState,
    onSignIn = viewModel::signIn,
    modifier = modifier,
  )
}
