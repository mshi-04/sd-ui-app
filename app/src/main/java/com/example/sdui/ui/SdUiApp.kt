package com.example.sdui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.ui.route.LoginRoute
import com.example.sdui.ui.route.MainRoute
import com.example.sdui.ui.screen.SignOutFailedScreen
import com.example.sdui.ui.screen.SigningOutScreen
import com.example.sdui.ui.state.AuthUiStatus
import com.example.sdui.ui.viewmodel.AuthViewModel

@Composable
fun SdUiApp(
  modifier: Modifier = Modifier,
  viewModel: AuthViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (uiState.status) {
    AuthUiStatus.SIGNING_OUT -> SigningOutScreen(modifier = modifier)
    AuthUiStatus.SIGN_OUT_FAILED ->
      SignOutFailedScreen(
        onRetry = viewModel::signOut,
        modifier = modifier,
      )
    AuthUiStatus.SIGNED_OUT ->
      LoginRoute(
        onSignedIn = viewModel::onSignedIn,
        modifier = modifier,
      )
    AuthUiStatus.SIGNED_IN ->
      MainRoute(
        accountName = uiState.accountName,
        onSessionEnd = viewModel::signOut,
        modifier = modifier,
      )
  }
}
