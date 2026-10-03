package com.example.sdui.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sdui.R
import com.example.sdui.ui.model.DashboardUiKind
import com.example.sdui.ui.model.DashboardUiModel
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.MainUiStatus
import com.example.sdui.ui.theme.SDUITheme

@Composable
fun MainScreen(
  uiState: MainUiState,
  accountName: String,
  onLoadUiSchema: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = if (uiState.compact) COMPACT_SPACING else COMFORTABLE_SPACING
  val darkTheme =
    if (uiState.status == MainUiStatus.LOADED) uiState.darkTheme else isSystemInDarkTheme()

  SDUITheme(darkTheme = darkTheme) {
    Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(spacing * 2),
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = stringResource(R.string.signed_in_as_format, accountName),
          style = MaterialTheme.typography.labelMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
          Button(
            onClick = onLoadUiSchema,
            enabled = uiState.status != MainUiStatus.LOADING,
          ) {
            Text(stringResource(R.string.load_ui_schema))
          }
          OutlinedButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out))
          }
        }
        UiSchemaContent(
          uiState = uiState,
          spacing = spacing,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }
  }
}

@Composable
private fun UiSchemaContent(
  uiState: MainUiState,
  spacing: Dp,
  modifier: Modifier = Modifier,
) {
  when (uiState.status) {
    MainUiStatus.LOADING ->
      Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    MainUiStatus.UNAUTHORIZED ->
      UiSchemaMessage(modifier = modifier, spacing = spacing) {
        Text(stringResource(R.string.ui_schema_unauthorized))
      }
    MainUiStatus.SURFACE_NOT_FOUND ->
      UiSchemaMessage(modifier = modifier, spacing = spacing) {
        Text(stringResource(R.string.ui_schema_surface_not_found))
        ApiBaseUrlCaption(apiBaseUrl = uiState.apiBaseUrl)
      }
    MainUiStatus.FAILED ->
      UiSchemaMessage(modifier = modifier, spacing = spacing) {
        Text(stringResource(R.string.ui_schema_failed))
        ApiBaseUrlCaption(apiBaseUrl = uiState.apiBaseUrl)
      }
    MainUiStatus.LOADED ->
      Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Text(
          text = stringResource(R.string.ui_schema_revision_format, uiState.revision),
          style = MaterialTheme.typography.labelMedium,
        )
        if (uiState.components.isEmpty()) {
          Text(stringResource(R.string.ui_schema_empty))
        }
        UiComponentList(components = uiState.components, spacing = spacing)
      }
  }
}

@Composable
private fun UiSchemaMessage(
  modifier: Modifier,
  spacing: Dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(spacing),
    content = content,
  )
}

@Composable
private fun ApiBaseUrlCaption(apiBaseUrl: String) {
  Text(
    text = stringResource(R.string.api_base_url_format, apiBaseUrl),
    style = MaterialTheme.typography.labelSmall,
  )
}

@Composable
private fun UiComponentList(components: List<DashboardUiModel>, spacing: Dp) {
  components.forEach { component ->
    UiComponentNode(
      component = component,
      spacing = spacing,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun UiComponentNode(
  component: DashboardUiModel,
  spacing: Dp,
  modifier: Modifier = Modifier,
) {
  when (component.kind) {
    DashboardUiKind.CARD ->
      Card(modifier = modifier) {
        UiComponentBody(component = component, spacing = spacing)
      }
    DashboardUiKind.PANEL ->
      OutlinedCard(modifier = modifier) {
        UiComponentBody(component = component, spacing = spacing)
      }
    DashboardUiKind.BANNER ->
      Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        UiComponentBody(component = component, spacing = spacing)
      }
    DashboardUiKind.CHART ->
      Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
      ) {
        UiComponentBody(component = component, spacing = spacing)
      }
    DashboardUiKind.LIST ->
      Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Text(text = component.label, style = MaterialTheme.typography.titleSmall)
        UiComponentList(components = component.children, spacing = spacing)
      }
    DashboardUiKind.BUTTON ->
      Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Button(onClick = {}, enabled = false) {
          Text(component.label)
        }
        UiComponentList(components = component.children, spacing = spacing)
      }
  }
}

@Composable
private fun UiComponentBody(component: DashboardUiModel, spacing: Dp) {
  Column(
    modifier = Modifier.padding(spacing),
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    Text(text = component.label, style = MaterialTheme.typography.titleMedium)
    UiComponentList(components = component.children, spacing = spacing)
  }
}

private val COMFORTABLE_SPACING = 16.dp
private val COMPACT_SPACING = 8.dp

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  MainScreen(
    uiState = MainUiState(),
    accountName = "plain@example.onmicrosoft.com",
    onLoadUiSchema = {},
    onSignOut = {},
  )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadedPreview() {
  MainScreen(
    uiState =
      MainUiState(
        status = MainUiStatus.LOADED,
        revision = "a1b2c3",
        components =
          listOf(
            DashboardUiModel(
              id = "billing-summary",
              kind = DashboardUiKind.CARD,
              label = "Billing",
              children =
                listOf(
                  DashboardUiModel(
                    id = "invoice-list",
                    kind = DashboardUiKind.LIST,
                    label = "invoices",
                    children = emptyList(),
                  ),
                ),
            ),
            DashboardUiModel(
              id = "impersonation-banner",
              kind = DashboardUiKind.BANNER,
              label = "warning",
              children = emptyList(),
            ),
          ),
      ),
    accountName = "billing@example.onmicrosoft.com",
    onLoadUiSchema = {},
    onSignOut = {},
  )
}
