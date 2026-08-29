package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerStatusMessageTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " ready "

    // Act
    // Normal: surrounding whitespace is removed
    val message = ServerStatusMessage(input)

    // Assert
    assertEquals("ready", message.value)
  }

  @Test
  fun normalConstructorKeepsOriginalCase() {
    // Arrange
    val input = "Ready"

    // Act
    // Normal: normalization is limited to trimming, so the case is preserved
    val message = ServerStatusMessage(input)

    // Assert
    assertEquals("Ready", message.value)
  }

  @Test
  fun boundaryConstructorRejectsWhitespaceOnlyValue() {
    // Arrange
    val input = " "

    // Act & Assert
    // Boundary: a whitespace-only value carries no message
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusMessage(input)
    }
  }

  @Test
  fun boundaryConstructorRejectsEmptyValue() {
    // Arrange
    val input = ""

    // Act & Assert
    // Boundary: an empty value carries no message
    assertThrows(IllegalArgumentException::class.java) {
      ServerStatusMessage(input)
    }
  }
}
