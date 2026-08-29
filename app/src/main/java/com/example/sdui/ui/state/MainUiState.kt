package com.example.sdui.ui.state

data class MainUiState(
  val schema: UiSchemaUiState = UiSchemaUiState.NotLoaded,
)

sealed interface UiSchemaUiState {
  data object NotLoaded : UiSchemaUiState

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
