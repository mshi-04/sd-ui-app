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
import com.example.sdui.ui.model.DashboardUiKind
import com.example.sdui.ui.model.DashboardUiModel
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.MainUiStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    if (_uiState.value.status == MainUiStatus.LOADING) return

    requestUiSchema()
  }

  private fun requestUiSchema() {
    requestJob?.cancel()
    _uiState.value = MainUiState(apiBaseUrl = BuildConfig.API_BASE_URL)
    requestJob =
      viewModelScope.launch {
        _uiState.value =
          loadUiSchemaUseCase().fold(
            onSuccess = { resolution ->
              when (resolution) {
                is UiSchemaResolution.Resolved ->
                  MainUiState(
                    apiBaseUrl = BuildConfig.API_BASE_URL,
                    status = MainUiStatus.LOADED,
                    revision = resolution.schema.revision,
                    darkTheme = resolution.schema.theme == UiTheme.DARK,
                    compact = resolution.schema.density == UiDensity.COMPACT,
                    components = resolution.schema.components.map(::dashboardUiModel),
                  )
                UiSchemaResolution.Unauthorized ->
                  MainUiState(
                    apiBaseUrl = BuildConfig.API_BASE_URL,
                    status = MainUiStatus.UNAUTHORIZED,
                  )
                UiSchemaResolution.SurfaceNotFound ->
                  MainUiState(
                    apiBaseUrl = BuildConfig.API_BASE_URL,
                    status = MainUiStatus.SURFACE_NOT_FOUND,
                  )
              }
            },
            onFailure = {
              MainUiState(apiBaseUrl = BuildConfig.API_BASE_URL, status = MainUiStatus.FAILED)
            },
          )
      }
  }

  private fun dashboardUiModel(component: UiComponent): DashboardUiModel =
    DashboardUiModel(
      id = component.id.value,
      kind =
        when (component.type) {
          UiComponentType.CARD -> DashboardUiKind.CARD
          UiComponentType.LIST -> DashboardUiKind.LIST
          UiComponentType.CHART -> DashboardUiKind.CHART
          UiComponentType.BUTTON -> DashboardUiKind.BUTTON
          UiComponentType.BANNER -> DashboardUiKind.BANNER
          UiComponentType.PANEL -> DashboardUiKind.PANEL
        },
      label =
        when (component.type) {
          UiComponentType.CARD -> component.props[TITLE_PROP]
          UiComponentType.PANEL -> component.props[TITLE_PROP] ?: component.props[SOURCE_PROP]
          UiComponentType.LIST -> component.props[SOURCE_PROP]
          UiComponentType.CHART -> component.props[METRIC_PROP]
          UiComponentType.BUTTON -> component.props[LABEL_PROP]
          UiComponentType.BANNER -> component.props[TONE_PROP]
        } ?: component.id.value,
      children = component.children.map(::dashboardUiModel),
    )

  private companion object {
    const val TITLE_PROP = "title"
    const val LABEL_PROP = "label"
    const val METRIC_PROP = "metric"
    const val SOURCE_PROP = "source"
    const val TONE_PROP = "tone"
  }
}
