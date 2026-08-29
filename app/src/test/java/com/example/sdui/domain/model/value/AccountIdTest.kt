package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountIdTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " 6f2c1d4e-0000-0000-0000-000000000000 "

    // Act
    // Normal: surrounding whitespace is removed
    val accountId = AccountId(input)

    // Assert
    assertEquals("6f2c1d4e-0000-0000-0000-000000000000", accountId.value)
  }

  @Test
  fun boundaryBlankInputIsRejected() {
    // Act & Assert
    // Boundary: a blank id cannot identify the signed in account
    assertThrows(IllegalArgumentException::class.java) { AccountId(" ") }
  }
}
