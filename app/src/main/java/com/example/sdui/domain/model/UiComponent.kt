package com.example.sdui.domain.model

import com.example.sdui.domain.model.value.UiComponentId

data class UiComponent(
  val id: UiComponentId,
  val type: UiComponentType,
  val props: Map<String, String>,
  val children: List<UiComponent>,
)
