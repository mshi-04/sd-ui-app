package com.example.sdui.data.remote

import com.example.sdui.data.model.HealthResponseDto
import retrofit2.http.GET

interface SdUiApi {
  @GET("health")
  suspend fun getHealth(): HealthResponseDto
}
