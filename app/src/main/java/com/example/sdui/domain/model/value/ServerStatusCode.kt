package com.example.sdui.domain.model.value

@JvmInline
value class ServerStatusCode private constructor(val value: String) {
  companion object {
    operator fun invoke(rawValue: String): ServerStatusCode {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "ServerStatusCode must not be blank." }
      return ServerStatusCode(normalizedValue)
    }
  }
}
