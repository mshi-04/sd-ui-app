package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.usecase.RestoreSessionUseCase
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.AuthUiState
import com.example.sdui.ui.state.AuthUiStatus
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
  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    restoreSession()
  }

  fun onSignedIn(accountName: String): Boolean {
    if (_uiState.value.status != AuthUiStatus.SIGNED_OUT) return false

    _uiState.value = AuthUiState(status = AuthUiStatus.SIGNED_IN, accountName = accountName)
    return true
  }

  fun signOut() {
    if (_uiState.value.status != AuthUiStatus.SIGNED_IN) return

    _uiState.value = AuthUiState(status = AuthUiStatus.RESTORING)
    viewModelScope.launch {
      signOutUseCase()
      _uiState.value = AuthUiState(status = AuthUiStatus.SIGNED_OUT)
    }
  }

  private fun restoreSession() {
    viewModelScope.launch {
      _uiState.value =
        restoreSessionUseCase().fold(
          onSuccess = { session ->
            when (session) {
              is AuthSession.SignedIn ->
                AuthUiState(
                  status = AuthUiStatus.SIGNED_IN,
                  accountName = session.account.name.value,
                )
              AuthSession.SignedOut -> AuthUiState(status = AuthUiStatus.SIGNED_OUT)
            }
          },
          onFailure = { AuthUiState(status = AuthUiStatus.SIGNED_OUT) },
        )
    }
  }
}
