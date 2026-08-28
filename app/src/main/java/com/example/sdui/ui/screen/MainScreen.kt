package com.example.sdui.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sdui.R
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.ServerConnectionUiState
import com.example.sdui.ui.theme.SDUITheme

@Composable
fun MainScreen(
  uiState: MainUiState,
  onCheckConnection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(text = stringResource(R.string.app_name))
    ConnectionStatus(connection = uiState.connection)
    Button(
      onClick = onCheckConnection,
      enabled = uiState.connection !is ServerConnectionUiState.Checking,
    ) {
      Text(stringResource(R.string.check_connection))
    }
  }
}

@Composable
private fun ConnectionStatus(connection: ServerConnectionUiState) {
  when (connection) {
    ServerConnectionUiState.NotChecked -> Text(stringResource(R.string.connection_not_checked))
    ServerConnectionUiState.Checking -> CircularProgressIndicator()
    is ServerConnectionUiState.Connected -> {
      Text(
        connection.message
          ?: stringResource(R.string.connection_status_format, connection.status),
      )
    }
    is ServerConnectionUiState.Unavailable -> {
      Text(
        connection.message
          ?: stringResource(R.string.connection_unavailable_format, connection.status),
      )
    }
    ServerConnectionUiState.Failed -> Text(stringResource(R.string.connection_failed))
  }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  SDUITheme {
    MainScreen(
      uiState = MainUiState(),
      onCheckConnection = {},
    )
  }
}
