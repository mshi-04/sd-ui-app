package com.example.sdui.data.remote

import com.example.sdui.data.model.UiSchemaDto
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.HttpException

class UiSchemaRemoteDataSource @Inject constructor(
  private val api: SdUiApi,
  private val json: Json,
) {
  suspend fun fetchUiSchema(surface: String, etag: String?): UiSchemaResponse {
    val response = api.getUiSchema(surface, etag)
    if (response.isSuccessful) {
      val responseBody = checkNotNull(response.body())
      return UiSchemaResponse.Schema(
        etag = response.headers()[ETAG_HEADER],
        body = decodeUiSchemaWithinNestingLimits(responseBody),
      )
    }
    return when (response.code()) {
      HttpURLConnection.HTTP_NOT_MODIFIED -> UiSchemaResponse.NotModified
      HttpURLConnection.HTTP_UNAUTHORIZED -> UiSchemaResponse.Unauthorized
      HttpURLConnection.HTTP_NOT_FOUND -> UiSchemaResponse.SurfaceNotFound
      else -> throw HttpException(response)
    }
  }

  private suspend fun decodeUiSchemaWithinNestingLimits(responseBody: ResponseBody): UiSchemaDto =
    withContext(Dispatchers.Default) {
      val bodyText = responseBody.use { it.string() }
      requireJsonBracketNestingWithinLimit(bodyText)
      val element = json.parseToJsonElement(bodyText)
      requireComponentDepthWithinLimit(element)
      json.decodeFromJsonElement(UiSchemaDto.serializer(), element)
    }

  // parseToJsonElement recurses on the call stack for object/array chains, so nesting is bounded on the raw text first
  private fun requireJsonBracketNestingWithinLimit(bodyText: String) {
    var nesting = 0
    var insideString = false
    var escaped = false
    for (character in bodyText) {
      if (insideString) {
        when {
          escaped -> escaped = false
          character == '\\' -> escaped = true
          character == '"' -> insideString = false
        }
        continue
      }
      when (character) {
        '"' -> insideString = true
        '{', '[' ->
          if (++nesting > MAX_JSON_BRACKET_NESTING) {
            throw SerializationException("JSON nesting exceeds $MAX_JSON_BRACKET_NESTING levels")
          }
        '}', ']' -> nesting--
      }
    }
  }

  // decodeFromJsonElement recurses on the call stack, so the depth is checked before it without recursion
  private fun requireComponentDepthWithinLimit(schemaElement: JsonElement) {
    var currentLevel: List<JsonElement> = childArrayOf(schemaElement, COMPONENTS_KEY)
    var depth = 1
    while (currentLevel.isNotEmpty()) {
      if (depth > MAX_COMPONENT_DEPTH) {
        throw SerializationException("Component nesting exceeds $MAX_COMPONENT_DEPTH levels")
      }
      currentLevel = currentLevel.flatMap { component -> childArrayOf(component, CHILDREN_KEY) }
      depth++
    }
  }

  private fun childArrayOf(element: JsonElement, key: String): List<JsonElement> =
    (element as? JsonObject)?.get(key) as? JsonArray ?: emptyList()

  private companion object {
    const val ETAG_HEADER = "ETag"
    const val COMPONENTS_KEY = "components"
    const val CHILDREN_KEY = "children"
    const val MAX_COMPONENT_DEPTH = 32
    const val MAX_JSON_BRACKET_NESTING = 128
  }
}
