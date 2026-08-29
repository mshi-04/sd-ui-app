package com.example.sdui.ui.state

data class UiComponentUiState(
  val id: String,
  val kind: UiComponentKindUiState,
  val label: String,
  val children: List<UiComponentUiState>,
)

enum class UiComponentKindUiState {
  CARD,
  LIST,
  CHART,
  BUTTON,
  BANNER,
  PANEL,
}
