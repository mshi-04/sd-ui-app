package com.example.sdui.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
  val status: String,
  val message: String? = null,
)
