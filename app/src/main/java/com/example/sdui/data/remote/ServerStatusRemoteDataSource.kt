package com.example.sdui.data.remote

import com.example.sdui.data.model.HealthResponseDto
import javax.inject.Inject

class ServerStatusRemoteDataSource @Inject constructor(
  private val api: SdUiApi,
) {
  suspend fun fetchStatus(): HealthResponseDto = api.getHealth()
}
