package com.example.sdui.domain.model

sealed interface ServerAvailability {
  val status: ServerStatus

  data class Available(
    override val status: ServerStatus,
  ) : ServerAvailability

  data class Unavailable(
    override val status: ServerStatus,
  ) : ServerAvailability
}
