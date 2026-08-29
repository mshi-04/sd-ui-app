package com.example.sdui.domain.model.value

@JvmInline
value class AccountId private constructor(val value: String) {
  companion object {
    operator fun invoke(rawValue: String): AccountId {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "AccountId must not be blank." }
      return AccountId(normalizedValue)
    }
  }
}
