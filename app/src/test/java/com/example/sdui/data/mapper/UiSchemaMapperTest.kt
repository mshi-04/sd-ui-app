package com.example.sdui.data.mapper

import com.example.sdui.data.model.UiComponentDto
import com.example.sdui.data.model.UiSchemaDto
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.model.value.UiComponentId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiSchemaMapperTest {
  @Test
  fun normalSchemaMetadataIsMappedToDomainValues() {
    // Arrange
    val dto = schemaDto(theme = "dark", density = "compact")

    // Act
    // Normal: the schema metadata is converted into domain values
    val actual = dto.toDomain()

    // Assert
    assertAll(
      { assertEquals(SurfaceId("dashboard"), actual.surface) },
      { assertEquals("2026-06-01", actual.version) },
      { assertEquals("a1b2c3", actual.revision) },
      { assertEquals(UiTheme.DARK, actual.theme) },
      { assertEquals(UiDensity.COMPACT, actual.density) },
    )
  }

  @Test
  fun boundaryMissingThemeAndDensityFallBackToServerDefaults() {
    // Arrange
    val dto = schemaDto(theme = null, density = null)

    // Act
    // Boundary: a response without presentation hints uses the documented defaults
    val actual = dto.toDomain()

    // Assert
    assertAll(
      { assertEquals(UiTheme.LIGHT, actual.theme) },
      { assertEquals(UiDensity.COMFORTABLE, actual.density) },
    )
  }

  @Test
  fun boundaryUnknownThemeAndDensityFallBackToServerDefaults() {
    // Arrange
    val dto = schemaDto(theme = "midnight", density = "roomy")

    // Act
    // Boundary: presentation values added later must not break an older client
    val actual = dto.toDomain()

    // Assert
    assertAll(
      { assertEquals(UiTheme.LIGHT, actual.theme) },
      { assertEquals(UiDensity.COMFORTABLE, actual.density) },
    )
  }

  @Test
  fun normalKnownComponentIsMappedWithItsChildren() {
    // Arrange
    val dto =
      schemaDto(
        components =
          listOf(
            componentDto(
              id = "billing-summary",
              type = "Card",
              props = buildJsonObject { put("title", "Billing") },
              children = listOf(componentDto(id = "invoice-list", type = "List")),
            ),
          ),
      )

    // Act
    // Normal: a known component keeps its identity, type and children
    val actual = dto.toDomain().components.single()

    // Assert
    assertAll(
      { assertEquals(UiComponentId("billing-summary"), actual.id) },
      { assertEquals(UiComponentType.CARD, actual.type) },
      { assertEquals(mapOf("title" to "Billing"), actual.props) },
      { assertEquals(UiComponentId("invoice-list"), actual.children.single().id) },
    )
  }

  @Test
  fun boundaryUnknownComponentTypeIsSkipped() {
    // Arrange
    val dto =
      schemaDto(
        components =
          listOf(
            componentDto(id = "timeline", type = "Timeline"),
            componentDto(id = "usage-summary", type = "Card"),
          ),
      )

    // Act
    // Boundary: a component type added by the server is dropped instead of failing the screen
    val actual = dto.toDomain().components

    // Assert
    assertEquals(listOf(UiComponentId("usage-summary")), actual.map { component -> component.id })
  }

  @Test
  fun boundaryUnknownChildComponentTypeIsSkipped() {
    // Arrange
    val dto =
      schemaDto(
        components =
          listOf(
            componentDto(
              id = "billing-summary",
              type = "Card",
              children = listOf(componentDto(id = "timeline", type = "Timeline")),
            ),
          ),
      )

    // Act
    // Boundary: the skip rule applies at every depth of the component tree
    val actual = dto.toDomain().components.single()

    // Assert
    assertTrue(actual.children.isEmpty())
  }

  @Test
  fun normalNonTextPropValuesAreExposedAsText() {
    // Arrange
    val props = buildJsonObject {
      put("title", "Billing")
      put("span", 6)
      put("pinned", true)
    }
    val dto = schemaDto(components = listOf(componentDto(type = "Card", props = props)))

    // Act
    // Normal: primitive props of any JSON type are carried as their textual content
    val actual = dto.toDomain().components.single().props

    // Assert
    assertEquals(mapOf("title" to "Billing", "span" to "6", "pinned" to "true"), actual)
  }

  @Test
  fun boundaryStructuredAndNullPropValuesAreDropped() {
    // Arrange
    val props = buildJsonObject {
      put("title", "Billing")
      put("subtitle", null as String?)
      putJsonObject("range") { put("from", "2026-01-01") }
    }
    val dto = schemaDto(components = listOf(componentDto(type = "Card", props = props)))

    // Act
    // Boundary: only primitive props survive the current textual prop model
    val actual = dto.toDomain().components.single().props

    // Assert
    assertEquals(mapOf("title" to "Billing"), actual)
  }

  @Test
  fun errorBlankComponentIdIsRejected() {
    // Arrange
    val dto = schemaDto(components = listOf(componentDto(id = " ", type = "Card")))

    // Act & Assert
    // Error: a value object constraint violation stops the conversion
    assertThrows(IllegalArgumentException::class.java) { dto.toDomain() }
  }

  private fun schemaDto(
    theme: String? = "dark",
    density: String? = "compact",
    components: List<UiComponentDto> = emptyList(),
  ): UiSchemaDto =
    UiSchemaDto(
      surface = "dashboard",
      version = "2026-06-01",
      revision = "a1b2c3",
      theme = theme,
      density = density,
      components = components,
    )

  private fun componentDto(
    id: String = "usage-summary",
    type: String = "Card",
    props: JsonObject = JsonObject(emptyMap()),
    children: List<UiComponentDto> = emptyList(),
  ): UiComponentDto = UiComponentDto(id = id, type = type, props = props, children = children)
}
