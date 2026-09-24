package com.example.sdui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sdui.BuildConfig
import com.example.sdui.domain.model.UiComponent
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.usecase.LoadUiSchemaUseCase
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.UiComponentKindUiState
import com.example.sdui.ui.state.UiComponentUiState
import com.example.sdui.ui.state.UiSchemaUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
  private val loadUiSchemaUseCase: LoadUiSchemaUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(MainUiState(apiBaseUrl = BuildConfig.API_BASE_URL))
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
  private var requestJob: Job? = null
  private var hasEnteredScreen = false

  init {
    requestUiSchema()
  }

  fun onScreenEntered() {
    if (!hasEnteredScreen) {
      hasEnteredScreen = true
      return
    }

    requestUiSchema()
  }

  fun loadUiSchema() {
    if (_uiState.value.schema is UiSchemaUiState.Loading) return

    requestUiSchema()
  }

  private fun requestUiSchema() {
    requestJob?.cancel()
    _uiState.update { state -> state.copy(schema = UiSchemaUiState.Loading) }
    requestJob =
      viewModelScope.launch {
        val schema =
          loadUiSchemaUseCase().fold(
            onSuccess = { resolution -> resolution.toUiState() },
            onFailure = { UiSchemaUiState.Failed },
          )
        _uiState.update { state -> state.copy(schema = schema) }
      }
  }

  private fun UiSchemaResolution.toUiState(): UiSchemaUiState =
    when (this) {
      is UiSchemaResolution.Resolved ->
        UiSchemaUiState.Loaded(
          revision = schema.revision,
          darkTheme = schema.theme == UiTheme.DARK,
          compact = schema.density == UiDensity.COMPACT,
          components = schema.components.map { component -> component.toUiState() },
        )
      UiSchemaResolution.Unauthorized -> UiSchemaUiState.Unauthorized
      UiSchemaResolution.SurfaceNotFound -> UiSchemaUiState.SurfaceNotFound
    }

  private fun UiComponent.toUiState(): UiComponentUiState =
    UiComponentUiState(
      id = id.value,
      kind = type.toUiState(),
      label = labelOrNull() ?: id.value,
      children = children.map { child -> child.toUiState() },
    )

  private fun UiComponent.labelOrNull(): String? =
    when (type) {
      UiComponentType.CARD -> props[TITLE_PROP]
      UiComponentType.PANEL -> props[TITLE_PROP] ?: props[SOURCE_PROP]
      UiComponentType.LIST -> props[SOURCE_PROP]
      UiComponentType.CHART -> props[METRIC_PROP]
      UiComponentType.BUTTON -> props[LABEL_PROP]
      UiComponentType.BANNER -> props[TONE_PROP]
    }

  private fun UiComponentType.toUiState(): UiComponentKindUiState =
    when (this) {
      UiComponentType.CARD -> UiComponentKindUiState.CARD
      UiComponentType.LIST -> UiComponentKindUiState.LIST
      UiComponentType.CHART -> UiComponentKindUiState.CHART
      UiComponentType.BUTTON -> UiComponentKindUiState.BUTTON
      UiComponentType.BANNER -> UiComponentKindUiState.BANNER
      UiComponentType.PANEL -> UiComponentKindUiState.PANEL
    }

  private companion object {
    const val TITLE_PROP = "title"
    const val LABEL_PROP = "label"
    const val METRIC_PROP = "metric"
    const val SOURCE_PROP = "source"
    const val TONE_PROP = "tone"
  }
}
