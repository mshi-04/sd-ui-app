package com.example.sdui.ui.route

import android.annotation.SuppressLint
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

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainRoute(
  accountName: String,
  onSessionEnd: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var hasEnteredScreen by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.onScreenEntered()
    hasEnteredScreen = true
  }

  LaunchedEffect(uiState.status, hasEnteredScreen) {
    if (
      hasEnteredScreen &&
        uiState.status == MainUiStatus.UNAUTHORIZED &&
        viewModel.uiState.value.status == MainUiStatus.UNAUTHORIZED
    ) {
      onSessionEnd()
    }
  }

  val currentUiState = viewModel.uiState.value
  val displayedUiState =
    if (!hasEnteredScreen || currentUiState.status == MainUiStatus.LOADING) {
      MainUiState(apiBaseUrl = uiState.apiBaseUrl)
    } else {
      uiState
    }

  MainScreen(
    uiState = displayedUiState,
    accountName = accountName,
    onLoadUiSchema = viewModel::loadUiSchema,
    onSignOut = onSessionEnd,
    modifier = modifier,
  )
}
