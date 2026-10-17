package com.example.sdui.domain.model.value

@JvmInline
value class AccountName private constructor(val value: String) {
  companion object {
    operator fun invoke(rawValue: String): AccountName {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "AccountName must not be blank." }
      return AccountName(normalizedValue)
    }
  }
}
