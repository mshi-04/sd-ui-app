package com.example.sdui.data.remote

import java.net.HttpURLConnection
import javax.inject.Inject
import retrofit2.HttpException

class UiSchemaRemoteDataSource @Inject constructor(
  private val api: SdUiApi,
) {
  suspend fun fetchUiSchema(surface: String, etag: String?): UiSchemaResponse {
    val response = api.getUiSchema(surface, etag)
    if (response.isSuccessful) {
      return UiSchemaResponse.Schema(
        etag = response.headers()[ETAG_HEADER],
        body = checkNotNull(response.body()),
      )
    }
    return when (response.code()) {
      HttpURLConnection.HTTP_NOT_MODIFIED -> UiSchemaResponse.NotModified
      HttpURLConnection.HTTP_UNAUTHORIZED -> UiSchemaResponse.Unauthorized
      HttpURLConnection.HTTP_NOT_FOUND -> UiSchemaResponse.SurfaceNotFound
      else -> throw HttpException(response)
    }
  }

  private companion object {
    const val ETAG_HEADER = "ETag"
  }
}
