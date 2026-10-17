package com.example.sdui.data.remote

import com.example.sdui.BuildConfig
import com.example.sdui.data.auth.AccessTokenProvider
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor @Inject constructor(
  private val accessTokenProvider: AccessTokenProvider,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    if (!isApiRequest(request.url)) return chain.proceed(request)
    val accessToken = accessTokenProvider.accessTokenOrNull() ?: return chain.proceed(request)
    return chain.proceed(
      request.newBuilder()
        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
        .build(),
    )
  }

  private fun isApiRequest(url: HttpUrl): Boolean {
    val apiBaseUrl = BuildConfig.API_BASE_URL.toHttpUrl()
    return url.isHttps && url.host == apiBaseUrl.host && url.port == apiBaseUrl.port
  }

  private companion object {
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer "
  }
}
