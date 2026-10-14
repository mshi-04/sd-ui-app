package com.example.sdui.data.repository

import com.example.sdui.data.mapper.toDomain
import com.example.sdui.data.remote.UiSchemaRemoteDataSource
import com.example.sdui.data.remote.UiSchemaResponse
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.repository.UiSchemaRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class DefaultUiSchemaRepository @Inject constructor(
  private val remoteDataSource: UiSchemaRemoteDataSource,
) : UiSchemaRepository {
  private val cachedSchemas = ConcurrentHashMap<String, CachedUiSchema>()
  private val cacheGeneration = AtomicLong()
  // Makes the generation check and the write atomic against clearSchema
  private val cacheLock = Any()

  override suspend fun getUiSchema(surface: SurfaceId): Result<UiSchemaResolution> =
    try {
      Result.success(resolveUiSchema(surface))
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Result.failure(error)
    }

  override fun clearSchema() {
    synchronized(cacheLock) {
      cacheGeneration.incrementAndGet()
      cachedSchemas.clear()
    }
  }

  private suspend fun resolveUiSchema(surface: SurfaceId): UiSchemaResolution {
    val generationAtRequest = cacheGeneration.get()
    val cached = cachedSchemas[surface.value]
    val response = remoteDataSource.fetchUiSchema(surface.value, cached?.etag)
    return when (response) {
      is UiSchemaResponse.Schema -> {
        val schema = response.body.toDomain()
        synchronized(cacheLock) {
          if (cacheGeneration.get() == generationAtRequest) {
            cachedSchemas[surface.value] = CachedUiSchema(etag = response.etag, schema = schema)
          }
        }
        UiSchemaResolution.Resolved(schema)
      }
      UiSchemaResponse.NotModified -> UiSchemaResolution.Resolved(checkNotNull(cached).schema)
      UiSchemaResponse.Unauthorized -> UiSchemaResolution.Unauthorized
      UiSchemaResponse.SurfaceNotFound -> UiSchemaResolution.SurfaceNotFound
    }
  }
}

private data class CachedUiSchema(
  val etag: String?,
  val schema: UiSchema,
)
