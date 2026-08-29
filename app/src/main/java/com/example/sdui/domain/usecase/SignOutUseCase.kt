package com.example.sdui.domain.usecase

import com.example.sdui.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
  private val repository: AuthRepository,
) {
  suspend operator fun invoke(): Result<Unit> = repository.signOut()
}
