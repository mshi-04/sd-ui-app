package com.example.sdui.data.remote

import com.example.sdui.BuildConfig
import com.example.sdui.data.auth.AccessTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.net.HttpURLConnection
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthorizationInterceptorTest {
  private val apiBaseUrl = BuildConfig.API_BASE_URL.toHttpUrl()
  private val httpsApiUrl = apiBaseUrl.newBuilder().scheme("https").port(apiBaseUrl.port).build()
  private val httpApiUrl = apiBaseUrl.newBuilder().scheme("http").port(apiBaseUrl.port).build()

  @Test
  fun normalAccessTokenIsSentAsBearerCredentialsToTheApiOverHttps() {
    // Arrange
    val accessTokenProvider = FakeAccessTokenProvider("token-value")
    val interceptor = AuthorizationInterceptor(accessTokenProvider)

    // Act
    // Normal: an available token is attached as bearer credentials
    val request = interceptAndTakeProceededRequest(interceptor, httpsApiUrl)

    // Assert
    assertEquals("Bearer token-value", request.header("Authorization"))
  }

  @Test
  fun boundaryMissingAccessTokenLeavesTheRequestUnauthenticated() {
    // Arrange
    val interceptor = AuthorizationInterceptor(FakeAccessTokenProvider(accessToken = null))

    // Act
    // Boundary: without a token the server decides, so the request is sent as is
    val request = interceptAndTakeProceededRequest(interceptor, httpsApiUrl)

    // Assert
    assertNull(request.header("Authorization"))
  }

  @Test
  fun boundaryHttpRequestToTheApiHostDoesNotReceiveTheToken() {
    // Arrange
    val accessTokenProvider = FakeAccessTokenProvider("token-value")
    val interceptor = AuthorizationInterceptor(accessTokenProvider)

    // Act
    // Boundary: a cleartext request never carries the token and does not even ask for it
    val request = interceptAndTakeProceededRequest(interceptor, httpApiUrl)

    // Assert
    assertAll(
      { assertNull(request.header("Authorization")) },
      { assertEquals(0, accessTokenProvider.requestCount) },
    )
  }

  @Test
  fun boundaryRequestToAnotherHostDoesNotReceiveTheToken() {
    // Arrange
    val accessTokenProvider = FakeAccessTokenProvider("token-value")
    val interceptor = AuthorizationInterceptor(accessTokenProvider)
    val otherHostUrl = httpsApiUrl.newBuilder().host("other.example.com").build()

    // Act
    // Boundary: a different host never carries the token and does not even ask for it
    val request = interceptAndTakeProceededRequest(interceptor, otherHostUrl)

    // Assert
    assertAll(
      { assertNull(request.header("Authorization")) },
      { assertEquals(0, accessTokenProvider.requestCount) },
    )
  }

  @Test
  fun boundaryRequestToAnotherPortDoesNotReceiveTheToken() {
    // Arrange
    val accessTokenProvider = FakeAccessTokenProvider("token-value")
    val interceptor = AuthorizationInterceptor(accessTokenProvider)
    val otherPortUrl = httpsApiUrl.newBuilder().port(httpsApiUrl.port + 1).build()

    // Act
    // Boundary: the same host on a different port never carries the token and does not even ask for it
    val request = interceptAndTakeProceededRequest(interceptor, otherPortUrl)

    // Assert
    assertAll(
      { assertNull(request.header("Authorization")) },
      { assertEquals(0, accessTokenProvider.requestCount) },
    )
  }

  private fun interceptAndTakeProceededRequest(
    interceptor: AuthorizationInterceptor,
    url: HttpUrl,
  ): Request {
    val proceededRequest = slot<Request>()
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns Request.Builder().url(url).build()
    every { chain.proceed(capture(proceededRequest)) } answers { okResponse(firstArg()) }

    interceptor.intercept(chain)
    return proceededRequest.captured
  }

  private fun okResponse(request: Request): Response =
    Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(HttpURLConnection.HTTP_OK)
      .message("OK")
      .build()
}

private class FakeAccessTokenProvider(
  private val accessToken: String?,
) : AccessTokenProvider {
  var requestCount = 0
    private set

  override fun accessTokenOrNull(): String? {
    requestCount++
    return accessToken
  }
}
