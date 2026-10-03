package com.example.sdui.ui.model

data class DashboardUiModel(
  val id: String,
  val kind: DashboardUiKind,
  val label: String,
  val children: List<DashboardUiModel>,
)

enum class DashboardUiKind {
  CARD,
  LIST,
  CHART,
  BUTTON,
  BANNER,
  PANEL,
}
