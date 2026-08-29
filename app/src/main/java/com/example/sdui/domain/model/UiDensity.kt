package com.example.sdui.domain.model

enum class UiDensity(val wireValue: String) {
  COMFORTABLE("comfortable"),
  COMPACT("compact"),
  ;

  companion object {
    val DEFAULT = COMFORTABLE

    fun fromWireValueOrDefault(rawValue: String?): UiDensity =
      entries.firstOrNull { density -> density.wireValue == rawValue } ?: DEFAULT
  }
}
