package com.example.sdui.data.remote

import com.example.sdui.data.model.HealthResponseDto
import com.example.sdui.data.repository.DefaultServerStatusRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SdUiApiTest {
  private val server = MockWebServer()
  private val json = Json { ignoreUnknownKeys = true }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `health response JSON is deserialized`() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"status":"ok","message":"ready"}"""),
    )
    val api = createApi()

    val actual = api.getHealth()

    assertEquals(HealthResponseDto(status = "ok", message = "ready"), actual)
  }

  @Test
  fun `health endpoint uses GET request`() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"status":"ok"}"""),
    )
    val api = createApi()

    api.getHealth()
    val request = server.takeRequest()

    assertEquals(RequestSnapshot(method = "GET", path = "/health"), RequestSnapshot(request.method, request.path))
  }

  @Test
  fun `missing required status becomes repository failure`() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{}"),
    )
    val repository = DefaultServerStatusRepository(ServerStatusRemoteDataSource(createApi()))

    val result = repository.getServerStatus()

    assertTrue(result.isFailure)
  }

  @Test
  fun `http error becomes repository failure`() = runTest {
    server.enqueue(MockResponse().setResponseCode(500))
    val repository = DefaultServerStatusRepository(ServerStatusRemoteDataSource(createApi()))

    val result = repository.getServerStatus()

    assertTrue(result.isFailure)
  }

  private fun createApi(): SdUiApi =
    Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(SdUiApi::class.java)
}

private data class RequestSnapshot(
  val method: String?,
  val path: String?,
)
