package com.example.sdui.ui.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.ui.screen.MainScreen
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.MainUiStatus
import com.example.sdui.ui.viewmodel.MainViewModel

@Composable
fun MainRoute(
  accountName: String,
  onSessionEnd: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = hiltViewModel(),
) {
  var hasEnteredScreen by remember(viewModel) { mutableStateOf(false) }

  LaunchedEffect(viewModel) {
    viewModel.onScreenEntered()
    hasEnteredScreen = true
  }

  // Reentry must reset the retained ViewModel before collecting the previous account's state.
  val uiState =
    if (hasEnteredScreen) {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      state
    } else {
      MainUiState()
    }

  LaunchedEffect(uiState.status) {
    if (uiState.status == MainUiStatus.UNAUTHORIZED) {
      onSessionEnd()
    }
  }

  MainScreen(
    uiState = uiState,
    accountName = accountName,
    onLoadUiSchema = viewModel::loadUiSchema,
    onSignOut = onSessionEnd,
    modifier = modifier,
  )
}
