package com.example.sdui.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UiSchemaDto(
  val surface: String,
  val version: String,
  val revision: String,
  val theme: String? = null,
  val density: String? = null,
  val components: List<UiComponentDto> = emptyList(),
)

@Serializable
data class UiComponentDto(
  val id: String,
  val type: String,
  val props: JsonObject = JsonObject(emptyMap()),
  val children: List<UiComponentDto> = emptyList(),
)
