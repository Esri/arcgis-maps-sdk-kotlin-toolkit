/**
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint
import com.arcgismaps.toolkit.utilitynetworks.internal.util.AdvancedOptionsRow
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ColorPicker
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ExpandableCardWithLabel
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.rememberExpandableCardState
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityNetwork

/**
 * A composable used to run a trace on a [UtilityNetwork].
 *
 * Provides options to select the trace configuration and add starting points for a trace.
 *
 * @since 200.6.0
 */
@Composable
internal fun TraceOptionsScreen(
    configurations: List<UtilityNamedTraceConfiguration>,
    startingPoints: List<StartingPoint>,
    selectedConfig: UtilityNamedTraceConfiguration?,
    defaultTraceName: String,
    selectedColor: Color,
    zoomToResult: Boolean,
    isTraceInProgress: Boolean,
    onStartingPointRemoved: (StartingPoint) -> Unit,
    onStartingPointSelected: (StartingPoint) -> Unit,
    onConfigSelected: (UtilityNamedTraceConfiguration) -> Unit,
    onTraceButtonClicked: () -> Unit,
    onAddStartingPointButtonClicked: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChanged: (Color) -> Unit,
    onZoomRequested: (Boolean) -> Unit
) {
    Column {
        var currentSelectedColor by remember { mutableStateOf(selectedColor) }

        LazyColumn(
            modifier = Modifier.weight(weight = 1f, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TraceConfigurations(
                    configurations,
                    selectedConfig,
                ) { newConfig ->
                    onConfigSelected(newConfig)
                }
            }
            item {
                StartingPoints(
                    startingPoints,
                    onAddStartingPointButtonClicked,
                    onStartingPointRemoved,
                    onStartingPointSelected,
                )
            }
            item {
                AdvancedOptions(
                    onNameChange = onNameChange,
                    onColorChanged = {
                        currentSelectedColor = it
                        onColorChanged(it)
                    },
                    defaultTraceName = defaultTraceName,
                    selectedColor = currentSelectedColor,
                    zoomToResult = zoomToResult,
                    onZoomRequested = onZoomRequested
                )
            }
        }
        TraceButton(
            enabled = selectedConfig != null && startingPoints.isNotEmpty() && isTraceInProgress.not(),
            onClicked = onTraceButtonClicked
        )
    }
}

@Composable
private fun TraceConfigurations(
    configs: List<UtilityNamedTraceConfiguration>,
    selectedConfig: UtilityNamedTraceConfiguration?,
    onTraceSelected: (UtilityNamedTraceConfiguration) -> Unit
) {
    TraceConfigurations(
        configs = configs.map { it.name },
        selectedConfigName = selectedConfig?.name ?: LocalContext.current.getString(R.string.no_configuration_selected)
    ) { index ->
        onTraceSelected(configs[index])
    }
}

@Composable
private fun TraceConfigurations(
    configs: List<String>,
    selectedConfigName: String,
    onTraceSelected: (Int) -> Unit
) {
    val expandableCardState = rememberExpandableCardState(false)
    var selectedConfigIndex by remember(selectedConfigName) { mutableIntStateOf(configs.indexOf(selectedConfigName)) }
    ExpandableCardWithLabel(
        expandableCardState = expandableCardState,
        labelText = stringResource(id = R.string.trace_configuration),
        contentTitle = selectedConfigName
    ) {
        Column {
            configs.forEachIndexed { index, name ->
                TraceConfiguration(name = name, isSelected = selectedConfigIndex == index) {
                    selectedConfigIndex = index
                    expandableCardState.toggle()
                    onTraceSelected(index)
                }
            }
        }
    }
}

@Composable
private fun TraceConfiguration(
    name: String, isSelected: Boolean, onClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClicked() }
    ) {
        ReadOnlyTextField(
            modifier = Modifier
                .padding(start = 5.dp)
                .height(40.dp),
            text = name,
            trailingIcon = {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(R.string.selected),
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TraceConfigsPreview() {
    TraceConfigurations(listOf("Foo", "Bar"), "Foo") {}
}

@Preview(showBackground = true)
@Composable
private fun AddStartingPointButtonPreview() {
    Column {
        AddStartingPointButton {

        }
    }
}

@Composable
private fun AddStartingPointButton(showAddStartingPointScreen: () -> Unit) {
    ElevatedButton(
        onClick = { showAddStartingPointScreen() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = ButtonDefaults.elevatedButtonColors()
            .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.add_starting_point),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A composable used to add starting points for the trace.
 *
 * @since 200.6.0
 */
@Composable
private fun StartingPoints(
    startingPoints: List<StartingPoint>,
    showAddStartingPointScreen: () -> Unit,
    onStartingPointRemoved: (StartingPoint) -> Unit,
    onStartingPointSelected: (StartingPoint) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddStartingPointButton {
            showAddStartingPointScreen()
        }
        ExpandableCard(
            title = "${stringResource(id = R.string.starting_points)} (${startingPoints.size})",
            padding = PaddingValues()
        ) {
            Column {
                startingPoints.forEach {
                    StartingPointRow(
                        data = it,
                        onDelete = { onStartingPointRemoved(it) },
                        onStartingPointSelected = onStartingPointSelected
                    )
                }
            }
        }
    }
}

/**
 * A composable used to display the advanced options
 *
 * @since 200.6.0
 */
@Composable
internal fun AdvancedOptions(
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    showZoomToResult: Boolean = true,
    defaultTraceName: String,
    selectedColor: Color,
    zoomToResult: Boolean,
    onNameChange: (String) -> Unit = {},
    onColorChanged: (Color) -> Unit = {},
    onZoomRequested: (Boolean) -> Unit = {}
) {
    ExpandableCard(
        title = stringResource(id = R.string.advanced_options),
        toggleable = true,
        expandableCardState = rememberExpandableCardState(isExpanded = false),
        padding = PaddingValues()
    ) {
        val zoomToResultDescription = stringResource(id = R.string.zoom_to_result_description)
        Column {
            if (showName) {
                val focusManager = LocalFocusManager.current
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var text by remember(defaultTraceName) { mutableStateOf(defaultTraceName) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { newValue ->
                            text = newValue
                            onNameChange(newValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.name)) },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        maxLines = 1,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Color picker
            AdvancedOptionsRow(name = stringResource(id = R.string.color)) {
                ColorPicker(selectedColor, onColorChanged)
            }

            if (showZoomToResult) {
                var isEnabled by rememberSaveable { mutableStateOf(zoomToResult) }
                val interactionSource = remember { MutableInteractionSource() }
                LaunchedEffect(Unit) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            isEnabled = !isEnabled
                        }
                    }
                }
                AdvancedOptionsRow(
                    name = stringResource(id = R.string.zoom_to_result),
                    modifier = Modifier
                        .clickable(interactionSource = interactionSource, indication = null) {}
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { newState ->
                            onZoomRequested(newState)
                        },
                        modifier = Modifier
                            .semantics { contentDescription = zoomToResultDescription }
                            .padding(horizontal = 4.dp),
                        enabled = true,
                        interactionSource = interactionSource
                    )
                }
            }
        }
    }
}

@Composable
private fun TraceButton(enabled: Boolean = true, onClicked: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Button(
            onClick = { onClicked() },
            enabled = enabled
        ) {
            Text(stringResource(id = R.string.trace))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdvancedOptionsPreview() {
    AdvancedOptions(defaultTraceName = "", selectedColor = Color.Green, zoomToResult = false)
}
