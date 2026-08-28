package com.example.sdui.domain.model.value

@JvmInline
value class ServerStatusMessage private constructor(val value: String) {
  companion object {
    operator fun invoke(rawValue: String): ServerStatusMessage {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "ServerStatusMessage must not be blank." }
      return ServerStatusMessage(normalizedValue)
    }
  }
}
