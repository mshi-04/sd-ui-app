package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.usecase.SignInUseCase
import com.example.sdui.ui.state.LoginUiState
import com.example.sdui.ui.state.SignInUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val signInUseCase: SignInUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun signIn() {
    if (_uiState.value.signIn is SignInUiState.InProgress) return

    _uiState.update { state -> state.copy(signIn = SignInUiState.InProgress) }
    viewModelScope.launch {
      val signIn =
        signInUseCase().fold(
          onSuccess = { session ->
            when (session) {
              is AuthSession.SignedIn -> SignInUiState.Succeeded
              AuthSession.SignedOut -> SignInUiState.Failed
            }
          },
          onFailure = { SignInUiState.Failed },
        )
      _uiState.update { state -> state.copy(signIn = signIn) }
    }
  }
}
