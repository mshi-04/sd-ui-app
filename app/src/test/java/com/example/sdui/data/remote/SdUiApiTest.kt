package com.example.sdui.data.remote

import com.example.sdui.data.di.NetworkModule
import com.example.sdui.data.model.UiComponentDto
import com.example.sdui.data.model.UiSchemaDto
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SdUiApiTest {
  private val server = MockWebServer()

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun normalUiSchemaResponseIsDeserialized() = runTest {
    // Arrange
    enqueueJson(SCHEMA_JSON)
    val api = createApi()

    // Act
    // Normal: the UI schema JSON is deserialized into the DTO tree
    val actual = api.getUiSchema(SURFACE, null).body()

    // Assert
    assertEquals(
      UiSchemaDto(
        surface = "dashboard",
        version = "2026-06-01",
        revision = "a1b2c3",
        theme = "dark",
        density = "compact",
        components =
          listOf(
            UiComponentDto(
              id = "billing-summary",
              type = "Card",
              props = buildJsonObject {
                put("title", "Billing")
                put("span", 6)
              },
              children =
                listOf(
                  UiComponentDto(
                    id = "invoice-list",
                    type = "List",
                    props = buildJsonObject { put("source", "invoices") },
                    children = emptyList(),
                  ),
                ),
            ),
          ),
      ),
      actual,
    )
  }

  @Test
  fun boundaryMissingOptionalFieldsFallBackToDefaults() = runTest {
    // Arrange
    enqueueJson("""{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3"}""")
    val api = createApi()

    // Act
    // Boundary: presentation hints and components are optional on the wire
    val actual = api.getUiSchema(SURFACE, null).body()

    // Assert
    assertEquals(
      UiSchemaDto(surface = "dashboard", version = "2026-06-01", revision = "a1b2c3"),
      actual,
    )
  }

  @Test
  fun boundaryUnknownFieldsAreIgnored() = runTest {
    // Arrange
    enqueueJson(
      """{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3","expiresIn":60}""",
    )
    val api = createApi()

    // Act
    // Boundary: fields the client does not know do not break the response
    val actual = api.getUiSchema(SURFACE, null).body()

    // Assert
    assertEquals(
      UiSchemaDto(surface = "dashboard", version = "2026-06-01", revision = "a1b2c3"),
      actual,
    )
  }

  @Test
  fun interactionSurfaceIsSentAsPathSegment() = runTest {
    // Arrange
    enqueueJson(SCHEMA_JSON)
    val api = createApi()

    // Act
    // Interaction: the call reaches the surface endpoint with GET
    api.getUiSchema(SURFACE, null)
    val request = takeRequest()

    // Assert
    assertAll(
      { assertEquals("GET", request.method) },
      { assertEquals("/ui/dashboard", request.path) },
    )
  }

  @Test
  fun interactionEtagIsSentAsConditionalRequestHeader() = runTest {
    // Arrange
    enqueueJson(SCHEMA_JSON)
    val api = createApi()

    // Act
    // Interaction: a known ETag turns the call into a conditional request
    api.getUiSchema(SURFACE, ETAG)
    val request = takeRequest()

    // Assert
    assertEquals(ETAG, request.getHeader("If-None-Match"))
  }

  @Test
  fun boundaryMissingEtagOmitsConditionalRequestHeader() = runTest {
    // Arrange
    enqueueJson(SCHEMA_JSON)
    val api = createApi()

    // Act
    // Boundary: without a cached ETag the request carries no validator
    api.getUiSchema(SURFACE, null)
    val request = takeRequest()

    // Assert
    assertNull(request.getHeader("If-None-Match"))
  }

  @Test
  fun boundaryNotModifiedResponseCarriesNoBody() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))
    val api = createApi()

    // Act
    // Boundary: a matched ETag answers with a status only
    val actual = api.getUiSchema(SURFACE, ETAG)

    // Assert
    assertAll(
      { assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, actual.code()) },
      { assertNull(actual.body()) },
    )
  }

  @Test
  fun errorRejectedRequestIsReportedAsUnsuccessfulResponse() = runTest {
    // Arrange
    server.enqueue(
      MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
        .setBody("""{"error":{"code":"unauthorized"}}"""),
    )
    val api = createApi()

    // Act
    // Error: HTTP failures reach the caller as a response instead of an exception
    val actual = api.getUiSchema(SURFACE, null)

    // Assert
    assertAll(
      { assertFalse(actual.isSuccessful) },
      { assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, actual.code()) },
    )
  }

  @Test
  fun errorMissingRequiredFieldFailsDeserialization() = runTest {
    // Arrange
    enqueueJson("""{"surface":"dashboard"}""")
    val api = createApi()

    // Act
    // Error: required schema fields have no default, so decoding fails
    val thrown = runCatching { api.getUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  private fun enqueueJson(body: String) {
    server.enqueue(
      MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeader("Content-Type", "application/json")
        .setBody(body),
    )
  }

  private fun takeRequest() =
    requireNotNull(server.takeRequest(TAKE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))

  private fun createApi(): SdUiApi {
    val json = NetworkModule.provideJson()
    val okHttpClient = NetworkModule.provideOkHttpClient(emptySet())
    val retrofit =
      NetworkModule.provideRetrofit(json, okHttpClient)
        .newBuilder()
        .baseUrl(server.url("/"))
        .build()
    return NetworkModule.provideSdUiApi(retrofit)
  }

  private companion object {
    const val SURFACE = "dashboard"
    const val ETAG = "\"a1b2c3d4\""
    const val TAKE_REQUEST_TIMEOUT_SECONDS = 1L
    const val SCHEMA_JSON =
      """{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3",""" +
        """"theme":"dark","density":"compact","components":[""" +
        """{"id":"billing-summary","type":"Card","props":{"title":"Billing","span":6},""" +
        """"children":[{"id":"invoice-list","type":"List",""" +
        """"props":{"source":"invoices"},"children":[]}]}]}"""
  }
}
