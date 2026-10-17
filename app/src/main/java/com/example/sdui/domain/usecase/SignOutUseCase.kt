package com.example.sdui.domain.usecase

import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.repository.UiSchemaRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
  private val authRepository: AuthRepository,
  private val uiSchemaRepository: UiSchemaRepository,
) {
  suspend operator fun invoke(): Result<Unit> =
    try {
      authRepository.signOut()
    } finally {
      uiSchemaRepository.clearSchema()
    }
}
