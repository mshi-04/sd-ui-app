package com.example.sdui.domain.model

import com.example.sdui.domain.model.value.SurfaceId

data class UiSchema(
  val surface: SurfaceId,
  val version: String,
  val revision: String,
  val theme: UiTheme,
  val density: UiDensity,
  val components: List<UiComponent>,
)
