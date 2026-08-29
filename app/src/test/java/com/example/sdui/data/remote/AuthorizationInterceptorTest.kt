package com.example.sdui.data.remote

import com.example.sdui.data.auth.AccessTokenProvider
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthorizationInterceptorTest {
  private val server = MockWebServer()

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun normalAccessTokenIsSentAsBearerCredentials() {
    // Arrange
    val client = createClient(devIdentityConfigured = false, accessToken = "token-value")

    // Act
    // Normal: an available token is attached as bearer credentials
    val request = executeAndTakeRequest(client)

    // Assert
    assertEquals("Bearer token-value", request.getHeader("Authorization"))
  }

  @Test
  fun boundaryMissingAccessTokenLeavesTheRequestUnauthenticated() {
    // Arrange
    val client = createClient(devIdentityConfigured = false, accessToken = null)

    // Act
    // Boundary: without a token the server decides, so the request is sent as is
    val request = executeAndTakeRequest(client)

    // Assert
    assertNull(request.getHeader("Authorization"))
  }

  @Test
  fun boundaryConfiguredDevIdentitySuppressesTheAuthorizationHeader() {
    // Arrange
    val client = createClient(devIdentityConfigured = true, accessToken = "token-value")

    // Act
    // Boundary: the local mock server is driven by X-Dev headers, so the two are exclusive
    val request = executeAndTakeRequest(client)

    // Assert
    assertNull(request.getHeader("Authorization"))
  }

  private fun executeAndTakeRequest(client: OkHttpClient) =
    client
      .newCall(Request.Builder().url(server.url("/api/ui/dashboard")).build())
      .execute()
      .use { requireNotNull(server.takeRequest(TAKE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) }

  private fun createClient(devIdentityConfigured: Boolean, accessToken: String?): OkHttpClient {
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"))
    return OkHttpClient.Builder()
      .addInterceptor(
        AuthorizationInterceptor(
          devIdentityConfigured = devIdentityConfigured,
          accessTokenProvider = FakeAccessTokenProvider(accessToken),
        ),
      )
      .build()
  }

  private companion object {
    const val TAKE_REQUEST_TIMEOUT_SECONDS = 1L
  }
}

private class FakeAccessTokenProvider(
  private val accessToken: String?,
) : AccessTokenProvider {
  override fun accessTokenOrNull(): String? = accessToken
}
