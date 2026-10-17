package com.example.sdui.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SurfaceIdTest {
  @Test
  fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
    // Arrange
    val input = " dashboard "

    // Act
    // Normal: surrounding whitespace is removed
    val surfaceId = SurfaceId(input)

    // Assert
    assertEquals("dashboard", surfaceId.value)
  }

  @Test
  fun boundaryBlankInputIsRejected() {
    // Act & Assert
    // Boundary: a blank surface would produce a request without a surface path segment
    assertThrows(IllegalArgumentException::class.java) { SurfaceId("  ") }
  }

  @Test
  fun normalDashboardConstantMatchesServerSurfaceName() {
    // Act
    // Normal: the only surface the server serves is addressed by its contract name
    val actual = SurfaceId.DASHBOARD

    // Assert
    assertEquals("dashboard", actual.value)
  }
}
