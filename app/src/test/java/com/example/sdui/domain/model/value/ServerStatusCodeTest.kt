package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerStatusCodeTest {
  @Test
  fun `constructor trims surrounding whitespace`() {
    val statusCode = ServerStatusCode(" ok ")

    assertEquals("ok", statusCode.value)
  }

  @Test
  fun `constructor rejects blank value`() {
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusCode(" ")
    }
  }
}
