package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.domain.model.ServerAvailability
import com.example.sdui.domain.usecase.CheckServerStatusUseCase
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.ServerConnectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
  private val checkServerStatusUseCase: CheckServerStatusUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  fun checkConnection() {
    if (_uiState.value.connection is ServerConnectionUiState.Checking) return

    _uiState.update { state -> state.copy(connection = ServerConnectionUiState.Checking) }
    viewModelScope.launch {
      val connection =
        checkServerStatusUseCase().fold(
          onSuccess = { availability ->
            when (availability) {
              is ServerAvailability.Available ->
                ServerConnectionUiState.Connected(
                  status = availability.status.code.value,
                  message = availability.status.message?.value,
                )
              is ServerAvailability.Unavailable ->
                ServerConnectionUiState.Unavailable(
                  status = availability.status.code.value,
                  message = availability.status.message?.value,
                )
            }
          },
          onFailure = { ServerConnectionUiState.Failed },
        )
      _uiState.update { state -> state.copy(connection = connection) }
    }
  }
}
