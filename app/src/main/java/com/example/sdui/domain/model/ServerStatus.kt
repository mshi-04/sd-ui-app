package com.example.sdui.domain.model

import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage

data class ServerStatus(
  val code: ServerStatusCode,
  val message: ServerStatusMessage?,
)
