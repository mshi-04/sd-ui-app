package com.example.sdui.domain.model

enum class UiComponentType(val wireValue: String) {
  CARD("Card"),
  LIST("List"),
  CHART("Chart"),
  BUTTON("Button"),
  BANNER("Banner"),
  PANEL("Panel"),
  ;

  companion object {
    fun fromWireValueOrNull(rawValue: String): UiComponentType? =
      entries.firstOrNull { type -> type.wireValue == rawValue }
  }
}
