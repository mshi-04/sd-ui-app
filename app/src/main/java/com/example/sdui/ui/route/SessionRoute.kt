package com.example.sdui.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sdui.R
import com.example.sdui.ui.state.SessionUiState
import com.example.sdui.ui.viewmodel.SessionViewModel

@Composable
fun SessionRoute(
  modifier: Modifier = Modifier,
  viewModel: SessionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (val session = uiState) {
    SessionUiState.Restoring -> RestoringSession(modifier = modifier)
    SessionUiState.SignedOut ->
      LoginRoute(
        onSignedIn = viewModel::onSignedIn,
        modifier = modifier,
      )
    is SessionUiState.SignedIn ->
      MainRoute(
        accountName = session.accountName,
        onSignOut = viewModel::signOut,
        onUnauthorized = viewModel::signOut,
        modifier = modifier,
      )
  }
}

@Composable
private fun RestoringSession(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
    Text(stringResource(R.string.session_restoring))
  }
}
