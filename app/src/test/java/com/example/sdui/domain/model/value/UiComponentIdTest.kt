package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UiComponentIdTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " billing-summary "

    // Act
    // Normal: surrounding whitespace is removed
    val componentId = UiComponentId(input)

    // Assert
    assertEquals("billing-summary", componentId.value)
  }

  @Test
  fun boundaryBlankInputIsRejected() {
    // Act & Assert
    // Boundary: a blank id cannot identify a component on the screen
    assertThrows(IllegalArgumentException::class.java) { UiComponentId(" ") }
  }
}
