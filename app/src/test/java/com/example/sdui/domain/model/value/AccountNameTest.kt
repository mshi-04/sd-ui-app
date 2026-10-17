package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountNameTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " plain@example.onmicrosoft.com "

    // Act
    // Normal: surrounding whitespace is removed
    val accountName = AccountName(input)

    // Assert
    assertEquals("plain@example.onmicrosoft.com", accountName.value)
  }

  @Test
  fun boundaryBlankInputIsRejected() {
    // Act & Assert
    // Boundary: a blank name would leave the screen without anything to show
    assertThrows(IllegalArgumentException::class.java) { AccountName("  ") }
  }
}
