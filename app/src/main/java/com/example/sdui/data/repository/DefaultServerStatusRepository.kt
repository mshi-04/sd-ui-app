package com.example.sdui.data.repository

import com.example.sdui.data.mapper.toDomain
import com.example.sdui.data.remote.ServerStatusRemoteDataSource
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.repository.ServerStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class DefaultServerStatusRepository @Inject constructor(
  private val remoteDataSource: ServerStatusRemoteDataSource,
) : ServerStatusRepository {
  override suspend fun getServerStatus(): Result<ServerStatus> =
    try {
      val response = remoteDataSource.fetchStatus()
      Result.success(response.toDomain())
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Result.failure(error)
    }
}
