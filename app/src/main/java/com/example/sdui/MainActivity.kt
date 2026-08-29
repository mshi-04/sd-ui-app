package com.example.sdui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.sdui.data.auth.SignInActivityHolder
import com.example.sdui.ui.SdUiApp
import com.example.sdui.ui.theme.SDUITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @Inject
  lateinit var signInActivityHolder: SignInActivityHolder

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    signInActivityHolder.attach(this)

    enableEdgeToEdge()
    setContent {
      SDUITheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          SdUiApp()
        }
      }
    }
  }

  override fun onDestroy() {
    signInActivityHolder.detach(this)
    super.onDestroy()
  }
}
