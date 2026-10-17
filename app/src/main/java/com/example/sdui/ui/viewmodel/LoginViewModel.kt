package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.usecase.SignInUseCase
import com.example.sdui.ui.state.LoginUiState
import com.example.sdui.ui.state.LoginUiStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val signInUseCase: SignInUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun signIn() {
    if (_uiState.value.status == LoginUiStatus.IN_PROGRESS) return

    _uiState.value = LoginUiState(status = LoginUiStatus.IN_PROGRESS)
    viewModelScope.launch {
      _uiState.value =
        signInUseCase().fold(
          onSuccess = { session ->
            when (session) {
              is AuthSession.SignedIn ->
                LoginUiState(
                  status = LoginUiStatus.SUCCEEDED,
                  accountName = session.account.name.value,
                )
              AuthSession.SignedOut -> LoginUiState(status = LoginUiStatus.FAILED)
            }
          },
          onFailure = { LoginUiState(status = LoginUiStatus.FAILED) },
        )
    }
  }

  fun onSignedInHandled() {
    if (_uiState.value.status != LoginUiStatus.SUCCEEDED) return

    _uiState.value = LoginUiState()
  }
}
