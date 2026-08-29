package com.example.sdui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.ui.route.LoginRoute
import com.example.sdui.ui.route.MainRoute
import com.example.sdui.ui.screen.AuthRestoringScreen
import com.example.sdui.ui.state.AuthUiState
import com.example.sdui.ui.viewmodel.AuthViewModel

@Composable
fun SdUiApp(
  modifier: Modifier = Modifier,
  viewModel: AuthViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (val auth = uiState) {
    AuthUiState.Restoring -> AuthRestoringScreen(modifier = modifier)
    AuthUiState.SignedOut ->
      LoginRoute(
        onSignedIn = viewModel::onSignedIn,
        modifier = modifier,
      )
    is AuthUiState.SignedIn ->
      MainRoute(
        accountName = auth.accountName,
        onSessionEnd = viewModel::signOut,
        modifier = modifier,
      )
  }
}
