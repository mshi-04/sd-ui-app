package com.example.sdui.ui.state

import com.example.sdui.ui.model.DashboardUiModel

data class MainUiState(
  val apiBaseUrl: String = "",
  val status: MainUiStatus = MainUiStatus.LOADING,
  val revision: String = "",
  val darkTheme: Boolean = false,
  val compact: Boolean = false,
  val components: List<DashboardUiModel> = emptyList(),
)

enum class MainUiStatus {
  LOADING,
  LOADED,
  UNAUTHORIZED,
  SURFACE_NOT_FOUND,
  FAILED,
}
