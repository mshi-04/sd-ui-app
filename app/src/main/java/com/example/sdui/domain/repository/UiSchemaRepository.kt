package com.example.sdui.domain.repository

import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.value.SurfaceId

interface UiSchemaRepository {
  suspend fun getUiSchema(surface: SurfaceId): Result<UiSchemaResolution>
}
