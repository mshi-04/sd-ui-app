package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.usecase.RestoreSessionUseCase
import com.example.sdui.domain.usecase.SignOutUseCase
import com.example.sdui.ui.state.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
  private val restoreSessionUseCase: RestoreSessionUseCase,
  private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Restoring)
  val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

  init {
    restoreSession()
  }

  fun onSignedIn() {
    restoreSession()
  }

  fun signOut() {
    if (_uiState.value !is SessionUiState.SignedIn) return

    _uiState.value = SessionUiState.Restoring
    viewModelScope.launch {
      signOutUseCase()
      _uiState.value = SessionUiState.SignedOut
    }
  }

  private fun restoreSession() {
    _uiState.value = SessionUiState.Restoring
    viewModelScope.launch {
      _uiState.value =
        restoreSessionUseCase().fold(
          onSuccess = { session -> session.toUiState() },
          onFailure = { SessionUiState.SignedOut },
        )
    }
  }

  private fun AuthSession.toUiState(): SessionUiState =
    when (this) {
      is AuthSession.SignedIn -> SessionUiState.SignedIn(accountName = account.name.value)
      AuthSession.SignedOut -> SessionUiState.SignedOut
    }
}
