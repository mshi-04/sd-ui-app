package com.example.sdui.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface SdUiApi {
  @GET("ui/{surface}")
  suspend fun getUiSchema(
    @Path("surface") surface: String,
    @Header("If-None-Match") etag: String?,
  ): Response<ResponseBody>
}
