package com.example.sdui.domain.repository

import com.example.sdui.domain.model.ServerStatus

interface ServerStatusRepository {
  suspend fun getServerStatus(): Result<ServerStatus>
}
