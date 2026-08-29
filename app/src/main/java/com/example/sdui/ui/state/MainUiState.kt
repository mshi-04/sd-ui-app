package com.example.sdui.ui.state

data class MainUiState(
  val apiBaseUrl: String = "",
  val schema: UiSchemaUiState = UiSchemaUiState.Loading,
)

sealed interface UiSchemaUiState {
  data object Loading : UiSchemaUiState

  data class Loaded(
    val revision: String,
    val darkTheme: Boolean,
    val compact: Boolean,
    val components: List<UiComponentUiState>,
  ) : UiSchemaUiState

  data object Unauthorized : UiSchemaUiState

  data object SurfaceNotFound : UiSchemaUiState

  data object Failed : UiSchemaUiState
}
