package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerStatusMessageTest {
  @Test
  fun `constructor trims surrounding whitespace`() {
    val message = ServerStatusMessage(" ready ")

    assertEquals("ready", message.value)
  }

  @Test
  fun `constructor rejects blank value`() {
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusMessage(" ")
    }
  }
}
