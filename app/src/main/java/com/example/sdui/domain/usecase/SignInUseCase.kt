package com.example.sdui.domain.usecase

import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
  private val repository: AuthRepository,
) {
  suspend operator fun invoke(): Result<AuthSession> = repository.signIn()
}
