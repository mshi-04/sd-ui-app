package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerStatusCodeTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " ok "

    // Act
    // Normal: surrounding whitespace is removed
    val statusCode = ServerStatusCode(input)

    // Assert
    assertEquals("ok", statusCode.value)
  }

  @Test
  fun normalConstructorKeepsOriginalCase() {
    // Arrange
    val input = "OK"

    // Act
    // Normal: normalization is limited to trimming, so the case is preserved
    val statusCode = ServerStatusCode(input)

    // Assert
    assertEquals("OK", statusCode.value)
  }

  @Test
  fun boundaryConstructorRejectsWhitespaceOnlyValue() {
    // Arrange
    val input = " "

    // Act & Assert
    // Boundary: a whitespace-only value carries no status
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusCode(input)
    }
  }

  @Test
  fun boundaryConstructorRejectsEmptyValue() {
    // Arrange
    val input = ""

    // Act & Assert
    // Boundary: an empty value carries no status
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusCode(input)
    }
  }
}
