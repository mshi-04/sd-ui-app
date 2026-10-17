package com.example.sdui.domain.repository

import com.example.sdui.domain.model.AuthSession

interface AuthRepository {
  suspend fun signIn(): Result<AuthSession>

  suspend fun signOut(): Result<Unit>
}
