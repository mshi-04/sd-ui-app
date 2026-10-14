package com.example.sdui.data.remote

import com.example.sdui.data.di.NetworkModule
import com.example.sdui.data.model.UiComponentDto
import com.example.sdui.data.model.UiSchemaDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers.Companion.headersOf
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import retrofit2.Response

class UiSchemaRemoteDataSourceTest {
  private val api = mockk<SdUiApi>()
  private val json = NetworkModule.provideJson()

  @Test
  fun normalUiSchemaResponseIsDecodedIntoDtoTree() = runTest {
    // Arrange
    stubSchemaBody(SCHEMA_JSON)
    val dataSource = createDataSource()

    // Act
    // Normal: the UI schema JSON is decoded into the DTO tree
    val actual = dataSource.fetchUiSchema(SURFACE, null)

    // Assert
    assertEquals(
      UiSchemaResponse.Schema(
        etag = ETAG,
        body =
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
      ),
      actual,
    )
  }

  @Test
  fun boundaryMissingOptionalFieldsFallBackToDefaults() = runTest {
    // Arrange
    stubSchemaBody("""{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3"}""")
    val dataSource = createDataSource()

    // Act
    // Boundary: presentation hints and components are optional on the wire
    val actual = dataSource.fetchUiSchema(SURFACE, null)

    // Assert
    assertEquals(
      UiSchemaResponse.Schema(
        etag = ETAG,
        body = UiSchemaDto(surface = "dashboard", version = "2026-06-01", revision = "a1b2c3"),
      ),
      actual,
    )
  }

  @Test
  fun boundaryUnknownFieldsAreIgnored() = runTest {
    // Arrange
    stubSchemaBody(
      """{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3","expiresIn":60}""",
    )
    val dataSource = createDataSource()

    // Act
    // Boundary: fields the client does not know do not break the response
    val actual = dataSource.fetchUiSchema(SURFACE, null)

    // Assert
    assertEquals(
      UiSchemaResponse.Schema(
        etag = ETAG,
        body = UiSchemaDto(surface = "dashboard", version = "2026-06-01", revision = "a1b2c3"),
      ),
      actual,
    )
  }

  @Test
  fun errorMissingRequiredFieldFailsDecoding() = runTest {
    // Arrange
    stubSchemaBody("""{"surface":"dashboard"}""")
    val dataSource = createDataSource()

    // Act
    // Error: required schema fields have no default, so decoding fails
    val thrown = runCatching { dataSource.fetchUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun errorTruncatedJsonBodyFailsParsing() = runTest {
    // Arrange
    stubSchemaBody("""{"surface":"dashboard","version":"1","revision":"r","components":[{"id":""")
    val dataSource = createDataSource()

    // Act
    // Error: a body cut off mid-document fails as a normal exception instead of crashing
    val thrown = runCatching { dataSource.fetchUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun errorNonObjectTopLevelElementFailsDecoding() = runTest {
    // Arrange
    stubSchemaBody("[]")
    val dataSource = createDataSource()

    // Act
    // Error: a schema must be a JSON object, so an array root is rejected by decoding
    val thrown = runCatching { dataSource.fetchUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun boundaryComponentsNestedAtMaximumDepthAreAccepted() = runTest {
    // Arrange
    stubSchemaBody(nestedSchemaJson(levels = MAX_DEPTH))
    val dataSource = createDataSource()

    // Act
    // Boundary: exactly 32 levels of nesting is still within the limit
    val actual = dataSource.fetchUiSchema(SURFACE, null)

    // Assert
    assertAll(
      { assertInstanceOf(UiSchemaResponse.Schema::class.java, actual) },
      { assertEquals(MAX_DEPTH, depthOf((actual as UiSchemaResponse.Schema).body)) },
    )
  }

  @Test
  fun errorComponentsNestedBeyondMaximumDepthFail() = runTest {
    // Arrange
    stubSchemaBody(nestedSchemaJson(levels = MAX_DEPTH + 1))
    val dataSource = createDataSource()

    // Act
    // Error: 33 levels of nesting exceeds the limit and is rejected as a normal exception
    val thrown = runCatching { dataSource.fetchUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun errorExtremelyDeepNestingFailsWithoutStackOverflow() = runTest {
    // Arrange
    stubSchemaBody(nestedSchemaJson(levels = EXTREME_DEPTH))
    val dataSource = createDataSource()

    // Act
    // Error: a 100,000-level payload is rejected before any recursive parsing can overflow the stack
    val thrown = runCatching { dataSource.fetchUiSchema(SURFACE, null) }.exceptionOrNull()

    // Assert
    assertInstanceOf(SerializationException::class.java, thrown)
  }

  @Test
  fun boundaryBracketsInsideStringsAreNotCountedAsNesting() = runTest {
    // Arrange
    val bracketsInTitle = "[".repeat(EXTREME_DEPTH) + "\\\"" + "{".repeat(EXTREME_DEPTH)
    stubSchemaBody(
      """{"surface":"dashboard","version":"1","revision":"r","components":[""" +
        """{"id":"c","type":"Card","props":{"title":"$bracketsInTitle"}}]}""",
    )
    val dataSource = createDataSource()

    // Act
    // Boundary: only structural brackets count, including after an escaped quote inside a string
    val actual = dataSource.fetchUiSchema(SURFACE, null)

    // Assert
    assertInstanceOf(UiSchemaResponse.Schema::class.java, actual)
  }

  private fun createDataSource(): UiSchemaRemoteDataSource = UiSchemaRemoteDataSource(api, json)

  private fun stubSchemaBody(body: String) {
    coEvery { api.getUiSchema(SURFACE, null) } returns
      Response.success(body.toResponseBody(), headersOf("ETag", ETAG))
  }

  private fun nestedSchemaJson(levels: Int): String {
    val component = StringBuilder()
    for (level in 1..levels) {
      component.append("""{"id":"c$level","type":"Card","children":[""")
    }
    repeat(levels) { component.append("]}") }
    return """{"surface":"dashboard","version":"1","revision":"r","components":[""" +
      component +
      "]}"
  }

  private fun depthOf(schema: UiSchemaDto): Int {
    var depth = 0
    var level = schema.components
    while (level.isNotEmpty()) {
      depth++
      level = level.first().children
    }
    return depth
  }

  private companion object {
    const val SURFACE = "dashboard"
    const val ETAG = "\"a1b2c3d4\""
    const val MAX_DEPTH = 32
    const val EXTREME_DEPTH = 100_000
    const val SCHEMA_JSON =
      """{"surface":"dashboard","version":"2026-06-01","revision":"a1b2c3",""" +
        """"theme":"dark","density":"compact","components":[""" +
        """{"id":"billing-summary","type":"Card","props":{"title":"Billing","span":6},""" +
        """"children":[{"id":"invoice-list","type":"List",""" +
        """"props":{"source":"invoices"},"children":[]}]}]}"""
  }
}
