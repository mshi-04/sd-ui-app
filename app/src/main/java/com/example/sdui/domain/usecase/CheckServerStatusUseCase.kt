package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.ServerAvailability
import com.example.sdui.domain.repository.ServerStatusRepository
import javax.inject.Inject

class CheckServerStatusUseCase @Inject constructor(
  private val repository: ServerStatusRepository,
) {
  suspend operator fun invoke(): Result<ServerAvailability> =
    repository.getServerStatus().map { status ->
      if (status.code.value.equals(AVAILABLE_STATUS, ignoreCase = true)) {
        ServerAvailability.Available(status)
      } else {
        ServerAvailability.Unavailable(status)
      }
    }

  private companion object {
    const val AVAILABLE_STATUS = "ok"
  }
}
