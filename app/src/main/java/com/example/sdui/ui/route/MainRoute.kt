package com.example.sdui.ui.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.ui.screen.MainScreen
import com.example.sdui.ui.viewmodel.MainViewModel

@Composable
fun MainRoute(
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  MainScreen(
    uiState = uiState,
    onCheckConnection = viewModel::checkConnection,
    modifier = modifier,
  )
}
