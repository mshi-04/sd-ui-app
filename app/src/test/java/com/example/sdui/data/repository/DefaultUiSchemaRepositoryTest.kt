package com.example.sdui.data.repository

import com.example.sdui.data.di.NetworkModule
import com.example.sdui.data.model.UiComponentDto
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers.Companion.headersOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultUiSchemaRepositoryTest {
  private val api = mockk<SdUiApi>()

  @Test
  fun normalSuccessfulResponseIsMappedToResolvedSchema() = runTest {
    // Arrange
    coEvery { api.getUiSchema(SURFACE, null) } returns schemaResponse()
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
    coEvery { api.getUiSchema(SURFACE, null) } returns schemaResponse()
    coEvery { api.getUiSchema(SURFACE, ETAG) } returns notModifiedResponse()
    val repository = createRepository()

    // Act
    // Interaction: the ETag from the first response is replayed as a conditional request
    repository.getUiSchema(SurfaceId.DASHBOARD)
    repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    coVerify(exactly = 1) { api.getUiSchema(SURFACE, ETAG) }
  }

  @Test
  fun interactionRequestAfterClearSchemaCarriesNoEtag() = runTest {
    // Arrange
    coEvery { api.getUiSchema(SURFACE, null) } returns schemaResponse()
    val repository = createRepository()
    repository.getUiSchema(SurfaceId.DASHBOARD)

    // Act
    // Interaction: the previous account's ETag is not replayed after the cache is cleared
    repository.clearSchema()
    repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    coVerify(exactly = 2) { api.getUiSchema(SURFACE, null) }
    coVerify(exactly = 0) { api.getUiSchema(SURFACE, ETAG) }
  }

  @Test
  fun coroutineFetchStartedBeforeClearSchemaDoesNotWriteBack() = runTest {
    // Arrange
    val inFlight = CompletableDeferred<Response<ResponseBody>>()
    coEvery { api.getUiSchema(SURFACE, null) } coAnswers { inFlight.await() }
    val repository = createRepository()
    val pending = launch(start = CoroutineStart.UNDISPATCHED) {
      repository.getUiSchema(SurfaceId.DASHBOARD)
    }

    // Act
    // Coroutine: a response arriving after the cache was cleared is not stored
    repository.clearSchema()
    inFlight.complete(schemaResponse())
    pending.join()
    repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    coVerify(exactly = 2) { api.getUiSchema(SURFACE, null) }
    coVerify(exactly = 0) { api.getUiSchema(SURFACE, ETAG) }
  }

  @Test
  fun normalFetchAfterClearSchemaIsCachedAgain() = runTest {
    // Arrange
    coEvery { api.getUiSchema(SURFACE, null) } returns schemaResponse()
    coEvery { api.getUiSchema(SURFACE, ETAG) } returns notModifiedResponse()
    val repository = createRepository()
    repository.clearSchema()
    repository.getUiSchema(SurfaceId.DASHBOARD)

    // Act
    // Normal: clearing does not disable caching for later fetches
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD).getOrThrow()

    // Assert
    assertEquals(UiSchemaResolution.Resolved(expectedSchema()), actual)
    coVerify(exactly = 1) { api.getUiSchema(SURFACE, ETAG) }
  }

  @Test
  fun normalNotModifiedResponseKeepsTheCachedSchema() = runTest {
    // Arrange
    coEvery { api.getUiSchema(SURFACE, null) } returns schemaResponse()
    coEvery { api.getUiSchema(SURFACE, ETAG) } returns notModifiedResponse()
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
    coEvery { api.getUiSchema(SURFACE, null) } returns notModifiedResponse()
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
    coEvery { api.getUiSchema(SURFACE, null) } returns
      errorResponse(HttpURLConnection.HTTP_UNAUTHORIZED)
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
    coEvery { api.getUiSchema(SURFACE, null) } returns
      errorResponse(HttpURLConnection.HTTP_NOT_FOUND)
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
    coEvery { api.getUiSchema(SURFACE, null) } returns
      errorResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
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
    coEvery { api.getUiSchema(SURFACE, null) } throws IOException("connection reset")
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
    coEvery { api.getUiSchema(SURFACE, null) } throws CancellationException("cancelled")
    val repository = createRepository()

    // Act
    // Coroutine: cancellation is rethrown so the caller is not kept alive by a failed result
    val thrown = runCatching { repository.getUiSchema(SurfaceId.DASHBOARD) }.exceptionOrNull()

    // Assert
    assertInstanceOf(CancellationException::class.java, thrown)
  }

  @Test
  fun errorTooDeeplyNestedSchemaBecomesFailedResult() = runTest {
    // Arrange
    coEvery { api.getUiSchema(SURFACE, null) } returns
      Response.success(tooDeepSchemaBody(), headersOf("ETag", ETAG))
    val repository = createRepository()

    // Act
    // Error: a schema nested beyond the client limit is folded into a failed result
    val actual = repository.getUiSchema(SurfaceId.DASHBOARD)

    // Assert
    assertInstanceOf(SerializationException::class.java, actual.exceptionOrNull())
  }

  private fun createRepository(): DefaultUiSchemaRepository =
    DefaultUiSchemaRepository(UiSchemaRemoteDataSource(api, NetworkModule.provideJson()))

  private fun tooDeepSchemaBody(): ResponseBody {
    val nested = StringBuilder()
    repeat(TOO_DEEP_LEVELS) { nested.append("""{"id":"c","type":"Card","children":[""") }
    repeat(TOO_DEEP_LEVELS) { nested.append("]}") }
    return """{"surface":"dashboard","version":"1","revision":"r","components":[$nested]}"""
      .toResponseBody()
  }

  private fun schemaResponse(): Response<ResponseBody> =
    Response.success(
      NetworkModule.provideJson()
        .encodeToString(UiSchemaDto.serializer(), schemaDto())
        .toResponseBody(),
      headersOf("ETag", ETAG),
    )

  // Retrofit only accepts a 304 through the raw-response overload; Response.error(code, body) requires 400+
  private fun notModifiedResponse(): Response<ResponseBody> =
    Response.error(
      "".toResponseBody(),
      okhttp3.Response.Builder()
        .request(Request.Builder().url(REQUEST_URL).build())
        .protocol(Protocol.HTTP_1_1)
        .code(HttpURLConnection.HTTP_NOT_MODIFIED)
        .message("Not Modified")
        .build(),
    )

  private fun errorResponse(code: Int): Response<ResponseBody> =
    Response.error(code, "".toResponseBody())

  private fun schemaDto(): UiSchemaDto =
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
                ),
              ),
          ),
        ),
    )

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
    const val SURFACE = "dashboard"
    const val ETAG = "\"a1b2c3d4\""
    const val REQUEST_URL = "http://localhost/ui/dashboard"
    const val TOO_DEEP_LEVELS = 33
  }
}
