package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.usecase.RestoreSessionUseCase
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
  private val restoreSessionUseCase: RestoreSessionUseCase,
  private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Restoring)
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    restoreSession()
  }

  fun onSignedIn(accountName: String): Boolean {
    if (_uiState.value !is AuthUiState.SignedOut) return false

    _uiState.value = AuthUiState.SignedIn(accountName = accountName)
    return true
  }

  fun signOut() {
    if (_uiState.value !is AuthUiState.SignedIn) return

    _uiState.value = AuthUiState.Restoring
    viewModelScope.launch {
      signOutUseCase()
      _uiState.value = AuthUiState.SignedOut
    }
  }

  private fun restoreSession() {
    viewModelScope.launch {
      _uiState.value =
        restoreSessionUseCase().fold(
          onSuccess = { session -> session.toRestoredUiState() },
          onFailure = { AuthUiState.SignedOut },
        )
    }
  }

  private fun AuthSession.toRestoredUiState(): AuthUiState =
    when (this) {
      is AuthSession.SignedIn -> AuthUiState.SignedIn(accountName = account.name.value)
      AuthSession.SignedOut -> AuthUiState.SignedOut
    }
}
