package com.example.sdui.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sdui.R
import com.example.sdui.ui.state.MainUiState
import com.example.sdui.ui.state.UiComponentKindUiState
import com.example.sdui.ui.state.UiComponentUiState
import com.example.sdui.ui.state.UiSchemaUiState
import com.example.sdui.ui.theme.SDUITheme

@Composable
fun MainScreen(
  uiState: MainUiState,
  onLoadUiSchema: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val loaded = uiState.schema as? UiSchemaUiState.Loaded
  val spacing = if (loaded?.compact == true) COMPACT_SPACING else COMFORTABLE_SPACING

  SDUITheme(darkTheme = loaded?.darkTheme ?: isSystemInDarkTheme()) {
    Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing * 2),
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall,
        )
        Button(
          onClick = onLoadUiSchema,
          enabled = uiState.schema !is UiSchemaUiState.Loading,
        ) {
          Text(stringResource(R.string.load_ui_schema))
        }
        UiSchemaContent(schema = uiState.schema, spacing = spacing)
      }
    }
  }
}

@Composable
private fun UiSchemaContent(schema: UiSchemaUiState, spacing: Dp) {
  when (schema) {
    UiSchemaUiState.NotLoaded -> Text(stringResource(R.string.ui_schema_not_loaded))
    UiSchemaUiState.Loading -> CircularProgressIndicator()
    UiSchemaUiState.Unauthorized -> Text(stringResource(R.string.ui_schema_unauthorized))
    UiSchemaUiState.SurfaceNotFound -> Text(stringResource(R.string.ui_schema_surface_not_found))
    UiSchemaUiState.Failed -> Text(stringResource(R.string.ui_schema_failed))
    is UiSchemaUiState.Loaded -> {
      Text(
        text = stringResource(R.string.ui_schema_revision_format, schema.revision),
        style = MaterialTheme.typography.labelMedium,
      )
      if (schema.components.isEmpty()) {
        Text(stringResource(R.string.ui_schema_empty))
      }
      UiComponentList(components = schema.components, spacing = spacing)
    }
  }
}

@Composable
private fun UiComponentList(components: List<UiComponentUiState>, spacing: Dp) {
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
  component: UiComponentUiState,
  spacing: Dp,
  modifier: Modifier = Modifier,
) {
  when (component.kind) {
    UiComponentKindUiState.CARD ->
      Card(modifier = modifier) {
        UiComponentBody(component = component, spacing = spacing)
      }
    UiComponentKindUiState.PANEL ->
      OutlinedCard(modifier = modifier) {
        UiComponentBody(component = component, spacing = spacing)
      }
    UiComponentKindUiState.BANNER ->
      Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        UiComponentBody(component = component, spacing = spacing)
      }
    UiComponentKindUiState.CHART ->
      Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
      ) {
        UiComponentBody(component = component, spacing = spacing)
      }
    UiComponentKindUiState.LIST ->
      Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Text(text = component.label, style = MaterialTheme.typography.titleSmall)
        UiComponentList(components = component.children, spacing = spacing)
      }
    UiComponentKindUiState.BUTTON ->
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
private fun UiComponentBody(component: UiComponentUiState, spacing: Dp) {
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
    onLoadUiSchema = {},
  )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadedPreview() {
  MainScreen(
    uiState =
      MainUiState(
        schema =
          UiSchemaUiState.Loaded(
            revision = "a1b2c3",
            darkTheme = false,
            compact = false,
            components =
              listOf(
                UiComponentUiState(
                  id = "billing-summary",
                  kind = UiComponentKindUiState.CARD,
                  label = "Billing",
                  children =
                    listOf(
                      UiComponentUiState(
                        id = "invoice-list",
                        kind = UiComponentKindUiState.LIST,
                        label = "invoices",
                        children = emptyList(),
                      ),
                    ),
                ),
                UiComponentUiState(
                  id = "impersonation-banner",
                  kind = UiComponentKindUiState.BANNER,
                  label = "warning",
                  children = emptyList(),
                ),
              ),
          ),
      ),
    onLoadUiSchema = {},
  )
}
