package com.example.sdui.domain.model.value

@JvmInline
value class UiComponentId private constructor(val value: String) {
  companion object {
    operator fun invoke(rawValue: String): UiComponentId {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "UiComponentId must not be blank." }
      return UiComponentId(normalizedValue)
    }
  }
}
