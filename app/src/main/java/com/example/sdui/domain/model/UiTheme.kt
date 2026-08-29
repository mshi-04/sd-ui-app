package com.example.sdui.domain.model

enum class UiTheme(val wireValue: String) {
  LIGHT("light"),
  DARK("dark"),
  ;

  companion object {
    val DEFAULT = LIGHT

    fun fromWireValueOrDefault(rawValue: String?): UiTheme =
      entries.firstOrNull { theme -> theme.wireValue == rawValue } ?: DEFAULT
  }
}
