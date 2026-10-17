package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.repository.UiSchemaRepository
import javax.inject.Inject

class LoadUiSchemaUseCase @Inject constructor(
  private val repository: UiSchemaRepository,
) {
  suspend operator fun invoke(
    surface: SurfaceId = SurfaceId.DASHBOARD,
  ): Result<UiSchemaResolution> = repository.getUiSchema(surface)
}
