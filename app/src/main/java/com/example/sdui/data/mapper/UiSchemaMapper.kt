package com.example.sdui.data.mapper

import com.example.sdui.data.model.UiComponentDto
import com.example.sdui.data.model.UiSchemaDto
import com.example.sdui.domain.model.UiComponent
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.model.value.UiComponentId
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun UiSchemaDto.toDomain(): UiSchema =
  UiSchema(
    surface = SurfaceId(surface),
    version = version,
    revision = revision,
    theme = UiTheme.fromWireValueOrDefault(theme),
    density = UiDensity.fromWireValueOrDefault(density),
    components = components.toDomain(),
  )

private fun List<UiComponentDto>.toDomain(): List<UiComponent> =
  mapNotNull { component -> component.toDomainOrNull() }

private fun UiComponentDto.toDomainOrNull(): UiComponent? {
  val componentType = UiComponentType.fromWireValueOrNull(type) ?: return null
  return UiComponent(
    id = UiComponentId(id),
    type = componentType,
    props = props.toTextProps(),
    children = children.toDomain(),
  )
}

private fun JsonObject.toTextProps(): Map<String, String> =
  entries.mapNotNull { (key, value) ->
    val primitive = value as? JsonPrimitive ?: return@mapNotNull null
    if (primitive is JsonNull) null else key to primitive.content
  }.toMap()
