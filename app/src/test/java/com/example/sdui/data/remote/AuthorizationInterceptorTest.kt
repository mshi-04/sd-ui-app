package com.example.sdui.data.remote

import com.example.sdui.data.auth.AccessTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.net.HttpURLConnection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthorizationInterceptorTest {
  @Test
  fun normalAccessTokenIsSentAsBearerCredentials() {
    // Arrange
    val interceptor = createInterceptor(devIdentityConfigured = false, accessToken = "token-value")

    // Act
    // Normal: an available token is attached as bearer credentials
    val request = interceptAndTakeProceededRequest(interceptor)

    // Assert
    assertEquals("Bearer token-value", request.header("Authorization"))
  }

  @Test
  fun boundaryMissingAccessTokenLeavesTheRequestUnauthenticated() {
    // Arrange
    val interceptor = createInterceptor(devIdentityConfigured = false, accessToken = null)

    // Act
    // Boundary: without a token the server decides, so the request is sent as is
    val request = interceptAndTakeProceededRequest(interceptor)

    // Assert
    assertNull(request.header("Authorization"))
  }

  @Test
  fun boundaryConfiguredDevIdentitySuppressesTheAuthorizationHeader() {
    // Arrange
    val interceptor = createInterceptor(devIdentityConfigured = true, accessToken = "token-value")

    // Act
    // Boundary: the local mock server is driven by X-Dev headers, so the two are exclusive
    val request = interceptAndTakeProceededRequest(interceptor)

    // Assert
    assertNull(request.header("Authorization"))
  }

  private fun interceptAndTakeProceededRequest(interceptor: AuthorizationInterceptor): Request {
    val proceededRequest = slot<Request>()
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns Request.Builder().url(REQUEST_URL).build()
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

  private fun createInterceptor(
    devIdentityConfigured: Boolean,
    accessToken: String?,
  ): AuthorizationInterceptor =
    AuthorizationInterceptor(
      devIdentityConfigured = devIdentityConfigured,
      accessTokenProvider = FakeAccessTokenProvider(accessToken),
    )

  private companion object {
    const val REQUEST_URL = "http://localhost/api/ui/dashboard"
  }
}

private class FakeAccessTokenProvider(
  private val accessToken: String?,
) : AccessTokenProvider {
  override fun accessTokenOrNull(): String? = accessToken
}
