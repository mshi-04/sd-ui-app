package com.example.sdui.data.repository

import com.example.sdui.data.di.NetworkModule
import com.example.sdui.data.model.UiSchemaDto
import com.example.sdui.data.remote.SdUiApi
import com.example.sdui.data.remote.UiSchemaRemoteDataSource
import com.example.sdui.domain.model.UiComponent
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.model.value.UiComponentId
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultUiSchemaRepositoryTest {
  private val server = MockWebServer()

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun normalSuccessfulResponseIsMappedToResolvedSchema() = runTest {
    // Arrange
    enqueueSchema()
    val repository = createRepository()

    // Act
    // Normal: the served schema is converted into the domain model
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD).getOrThrow()

    // Assert
    assertEquals(UiSchemaResolution.Resolved(expectedSchema()), actual)
  }

  @Test
  fun interactionSecondRequestRevalidatesWithTheCachedEtag() = runTest {
    // Arrange
    enqueueSchema()
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))
    val repository = createRepository()

    // Act
    // Interaction: the ETag from the first response is replayed as a conditional request
    repository.getUiSchema(SurfaceId.DASHBOARD)
    repository.getUiSchema(SurfaceId.DASHBOARD)
    takeRequest()
    val revalidation = takeRequest()

    // Assert
    assertEquals(ETAG, revalidation.getHeader("If-None-Match"))
  }

  @Test
  fun normalNotModifiedResponseKeepsTheCachedSchema() = runTest {
    // Arrange
    enqueueSchema()
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))
    val repository = createRepository()

    // Act
    // Normal: an unchanged schema is served from the cache instead of being decoded again
    repository.getUiSchema(SurfaceId.DASHBOARD)
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD).getOrThrow()

    // Assert
    assertEquals(UiSchemaResolution.Resolved(expectedSchema()), actual)
  }

  @Test
  fun errorNotModifiedWithoutCachedSchemaBecomesFailedResult() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))
    val repository = createRepository()

    // Act
    // Error: a validated response with nothing to validate against cannot produce a schema
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    assertInstanceOf(IllegalStateException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun errorRejectedTokenIsReportedAsUnauthorized() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))
    val repository = createRepository()

    // Act
    // Error: a rejected token is a documented outcome rather than a transport failure
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD).getOrThrow()

    // Assert
    assertEquals(UiSchemaResolution.Unauthorized, actual)
  }

  @Test
  fun errorUnknownSurfaceIsReportedAsSurfaceNotFound() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
    val repository = createRepository()

    // Act
    // Error: an unknown surface is a documented outcome rather than a transport failure
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD).getOrThrow()

    // Assert
    assertEquals(UiSchemaResolution.SurfaceNotFound, actual)
  }

  @Test
  fun errorUndocumentedStatusBecomesFailedResult() = runTest {
    // Arrange
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR))
    val repository = createRepository()

    // Act
    // Error: a status outside the contract is folded into a failed result
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    assertInstanceOf(HttpException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun errorTransportFailureBecomesFailedResult() = runTest {
    // Arrange
    server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
    val repository = createRepository()

    // Act
    // Error: a transport failure is folded into a failed result instead of escaping
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    assertInstanceOf(IOException::class.java, actual.exceptionOrNull())
  }

  @Test
  fun coroutineCancellationIsRethrownInsteadOfBeingFolded() = runTest {
    // Arrange
    val repository = DefaultUiSchemaRepository(UiSchemaRemoteDataSource(CancellingSdUiApi()))

    // Act
    // Coroutine: cancellation is rethrown so the caller is not kept alive by a failed result
    val thrown = runCatching { repository.getUiSchema(SurfaceId.DASHBOARD) }.exceptionOrNull()

    // Assert
    assertInstanceOf(CancellationException::class.java, thrown)
  }

  private fun enqueueSchema() {
    server.enqueue(
      MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeader("Content-Type", "application/json")
        .setHeader("ETag", ETAG)
        .setBody(SCHEMA_JSON),
    )
  }

  private fun takeRequest() =
    requireNotNull(server.takeRequest(TAKE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))

  private fun createRepository(): DefaultUiSchemaRepository {
    val json = NetworkModule.provideJson()
    val okHttpClient = NetworkModule.provideOkHttpClient(emptySet())
    val retrofit =
      NetworkModule.provideRetrofit(json, okHttpClient)
        .newBuilder()
        .baseUrl(server.url("/"))
        .build()
    val api = NetworkModule.provideSdUiApi(retrofit)
    return DefaultUiSchemaRepository(UiSchemaRemoteDataSource(api))
  }

  private fun expectedSchema(): UiSchema =
    UiSchema(
      surface = SurfaceId("dashboard"),
      version = "2026-06-01",
      revision = "a1b2c3",
      theme = UiTheme.DARK,
      density = UiDensity.COMPACT,
      components =
        listOf(
          UiComponent(
            id = UiComponentId("billing-summary"),
            type = UiComponentType.CARD,
            props = mapOf("title" to "Billing", "span" to "6"),
            children =
              listOf(
                UiComponent(
                  id = UiComponentId("invoice-list"),
                  type = UiComponentType.LIST,
                  props = mapOf("source" to "invoices"),
                  children = emptyList(),
                ),
              ),
          ),
        ),
    )

  private companion object {
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

private class CancellingSdUiApi : SdUiApi {
  override suspend fun getUiSchema(surface: String, etag: String?): Response<UiSchemaDto> =
    throw CancellationException("cancelled")
}
