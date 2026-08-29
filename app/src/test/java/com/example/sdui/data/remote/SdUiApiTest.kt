package com.example.sdui.data.remote

import com.example.sdui.data.di.NetworkModule
import com.example.sdui.data.model.HealthResponseDto
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import retrofit2.HttpException

class SdUiApiTest {
  private val server = MockWebServer()

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun normalHealthResponseIsDeserialized() = runTest {
    // Arrange
    enqueueJson("""{"status":"ok","message":"ready"}""")
    val api = createApi()

    // Act
    // Normal: the health JSON is deserialized into the DTO
    val actual = api.getHealth()

    // Assert
    assertEquals(HealthResponseDto(status = "ok", message = "ready"), actual)
  }

  @Test
  fun boundaryMissingOptionalMessageIsDeserializedAsNull() = runTest {
    // Arrange
    enqueueJson("""{"status":"ok"}""")
    val api = createApi()

    // Act
    // Boundary: the optional message field falls back to its default
    val actual = api.getHealth()

    // Assert
    assertEquals(HealthResponseDto(status = "ok", message = null), actual)
  }

  @Test
  fun boundaryUnknownFieldsAreIgnored() = runTest {
    // Arrange
    enqueueJson("""{"status":"ok","message":"ready","uptimeSeconds":42}""")
    val api = createApi()

    // Act
    // Boundary: fields the client does not know do not break the response
    val actual = api.getHealth()

    // Assert
    assertEquals(HealthResponseDto(status = "ok", message = "ready"), actual)
  }

  @Test
  fun interactionHealthEndpointUsesGetRequest() = runTest {
    // Arrange
    enqueueJson("""{"status":"ok"}""")
    val api = createApi()

    // Act
    // Interaction: the call reaches the health endpoint with GET
    api.getHealth()
    val request = requireNotNull(server.takeRequest(TAKE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))

    // Assert
    assertAll(
      { assertEquals("GET", request.method) },
      { assertEquals("/health", request.path) },
    )
  }

  @Test
  fun errorMissingRequiredStatusFailsDeserialization() = runTest {
    // Arrange
    enqueueJson("{}")
    val api = createApi()

    // Act
    // Error: the required status field has no default, so decoding fails
    val thrown = runCatching { api.getHealth() }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun errorHttpErrorFailsCall() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(500))
    val api = createApi()

    // Act
    // Error: an HTTP error surfaces as an exception from the API boundary
    val thrown = runCatching { api.getHealth() }.exceptionOrNull()

    // Assert
    assertInstanceOf(HttpException::class.java, thrown)
  }

  private fun enqueueJson(body: String) {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body),
    )
  }

  private fun createApi(): SdUiApi {
    val json = NetworkModule.provideJson()
    val retrofit = NetworkModule.provideRetrofit(json, server.url("/").toString())
    return NetworkModule.provideSdUiApi(retrofit)
  }

  private companion object {
    const val TAKE_REQUEST_TIMEOUT_SECONDS = 1L
  }
}
