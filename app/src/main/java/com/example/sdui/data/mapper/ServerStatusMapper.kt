package com.example.sdui.data.mapper

import com.example.sdui.data.model.HealthResponseDto
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage

fun HealthResponseDto.toDomain(): ServerStatus =
  ServerStatus(
    code = ServerStatusCode(status),
    message = message.toServerStatusMessageOrNull(),
  )

private fun String?.toServerStatusMessageOrNull(): ServerStatusMessage? {
  if (isNullOrBlank()) return null
  return ServerStatusMessage(this)
}
