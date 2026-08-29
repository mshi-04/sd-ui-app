package com.example.sdui.data.mapper

import com.example.sdui.data.model.HealthResponseDto
import com.example.sdui.domain.model.ServerStatus
import com.example.sdui.domain.model.value.ServerStatusCode
import com.example.sdui.domain.model.value.ServerStatusMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerStatusMapperTest {
  @Test
  fun normalDtoIsMappedToTrimmedDomainValues() {
    // Arrange
    val dto = HealthResponseDto(status = " ok ", message = " ready ")

    // Act
    // Normal: both fields become value objects that own the trimming
    val actual = dto.toDomain()

    // Assert
    assertEquals(
      ServerStatus(
        code = ServerStatusCode("ok"),
        message = ServerStatusMessage("ready"),
      ),
      actual,
    )
  }

  @Test
  fun boundaryMissingMessageIsMappedToNull() {
    // Arrange
    val dto = HealthResponseDto(status = "ok", message = null)

    // Act
    // Boundary: an absent message stays absent instead of becoming an empty value
    val actual = dto.toDomain()

    // Assert
    assertNull(actual.message)
  }

  @Test
  fun boundaryBlankMessageIsMappedToNull() {
    // Arrange
    val dto = HealthResponseDto(status = "ok", message = " ")

    // Act
    // Boundary: a blank message carries no meaning and becomes null
    val actual = dto.toDomain()

    // Assert
    assertNull(actual.message)
  }

  @Test
  fun boundaryBlankStatusIsRejected() {
    // Arrange
    val dto = HealthResponseDto(status = " ", message = "ready")

    // Act & Assert
    // Boundary: the value object constraint stops a meaningless status at the data boundary
    assertThrows(IllegalArgumentException::class.java) {
      dto.toDomain()
    }
  }
}
