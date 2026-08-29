package com.example.sdui.domain.model

sealed interface UiSchemaResolution {
  data class Resolved(
    val schema: UiSchema,
  ) : UiSchemaResolution

  data object Unauthorized : UiSchemaResolution

  data object SurfaceNotFound : UiSchemaResolution
}
