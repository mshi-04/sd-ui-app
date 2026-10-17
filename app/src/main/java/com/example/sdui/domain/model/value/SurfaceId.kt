package com.example.sdui.domain.model.value

@JvmInline
value class SurfaceId private constructor(val value: String) {
  companion object {
    val DASHBOARD = SurfaceId("dashboard")

    operator fun invoke(rawValue: String): SurfaceId {
      val normalizedValue = rawValue.trim()
      require(normalizedValue.isNotEmpty()) { "SurfaceId must not be blank." }
      return SurfaceId(normalizedValue)
    }
  }
}
