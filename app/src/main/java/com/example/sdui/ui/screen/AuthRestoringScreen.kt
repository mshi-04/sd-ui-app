package com.example.sdui.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sdui.R
import com.example.sdui.ui.theme.SDUITheme

@Composable
fun AuthRestoringScreen(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
    Text(stringResource(R.string.session_restoring))
  }
}

@Preview(showBackground = true)
@Composable
private fun AuthRestoringScreenPreview() {
  SDUITheme {
    AuthRestoringScreen()
  }
}
