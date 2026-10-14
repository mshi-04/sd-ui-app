package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
  private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    clearSession()
  }

  fun onSignedIn(accountName: String): Boolean {
    if (_uiState.value.status != AuthUiStatus.SIGNED_OUT) return false

    _uiState.value = AuthUiState(status = AuthUiStatus.SIGNED_IN, accountName = accountName)
    return true
  }

  fun signOut() {
    if (_uiState.value.status !in SIGN_OUT_ACCEPTING_STATUSES) return

    clearSession()
  }

  private fun clearSession() {
    _uiState.value = AuthUiState(status = AuthUiStatus.SIGNING_OUT)
    viewModelScope.launch {
      val status =
        signOutUseCase().fold(
          onSuccess = { AuthUiStatus.SIGNED_OUT },
          onFailure = { AuthUiStatus.SIGN_OUT_FAILED },
        )
      _uiState.value = AuthUiState(status = status)
    }
  }

  private companion object {
    val SIGN_OUT_ACCEPTING_STATUSES = setOf(AuthUiStatus.SIGNED_IN, AuthUiStatus.SIGN_OUT_FAILED)
  }
}
