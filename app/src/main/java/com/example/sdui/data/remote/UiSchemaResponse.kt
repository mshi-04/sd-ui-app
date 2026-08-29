package com.example.sdui.data.remote

import com.example.sdui.data.model.UiSchemaDto

sealed interface UiSchemaResponse {
  data class Schema(
    val etag: String?,
    val body: UiSchemaDto,
  ) : UiSchemaResponse

  data object NotModified : UiSchemaResponse

  data object Unauthorized : UiSchemaResponse

  data object SurfaceNotFound : UiSchemaResponse
}
