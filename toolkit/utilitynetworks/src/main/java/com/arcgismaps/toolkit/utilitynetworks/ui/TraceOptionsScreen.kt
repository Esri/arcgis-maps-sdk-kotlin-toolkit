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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.ui.expandablecard.theme.LocalExpandableCardColorScheme
import com.arcgismaps.toolkit.ui.expandablecard.theme.LocalExpandableCardTypography
import com.arcgismaps.toolkit.utilitynetworks.R
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
    configurations: List<SelectableItem>,
    onPerformTraceButtonClicked: () -> Unit,
    onAddStartingPointButtonClicked: () -> Unit
) {
    val traceConfigurations = remember { mutableStateListOf<SelectableItem>() }
    traceConfigurations.addAll(configurations)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(
                modifier = Modifier
                    .padding(10.dp, 3.dp)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TraceConfiguration(
                        traceConfigurations
                    )
                }
                item {
                    StartingPointsEditor(onAddStartingPointButtonClicked)
                }
                item {
                    AdvancedOptions()
                }
            }
            Button(onClick = { onPerformTraceButtonClicked() }) {
                Text(stringResource(id = R.string.trace))
            }
        }
    }
}

/**
 * A composable used to display the available trace types.
 *
 * @since 200.6.0
 */
@Composable
private fun TraceConfiguration(utilityTraces: List<SelectableItem>) {
    ExpandableCard(
        title = stringResource(id = R.string.trace_configuration),
        padding = 4.dp
    ) {
        var selectedTrace: SelectableItem? by remember { mutableStateOf(null) }
        Column {
            utilityTraces.forEachIndexed { index, item ->
                ReadOnlyTextField(
                    text = item.title,
                    leadingIcon = if (item.title == selectedTrace?.title) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .clickable {
                            selectedTrace = utilityTraces[index]
                        }
                )
            }
        }
    }
}

/**
 * A composable used to add starting points for the trace.
 *
 * @since 200.6.0
 */
@Composable
private fun StartingPointsEditor(showAddStartingPointScreen: () -> Unit) {
    val startingPoints = remember { mutableStateListOf(StartingPointData(name = "Test Starting Point")) }
    var counter by remember { mutableIntStateOf(1) }
    ExpandableCard(
        title = "${stringResource(id = R.string.starting_points)} (${counter})",
        description = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ElevatedButton(
                    onClick = {
                        startingPoints.add(StartingPointData("Point ${counter++}"))
                        showAddStartingPointScreen()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.add_starting_point),
                        color = LocalExpandableCardColorScheme.current.headerTextColor,
                        style = LocalExpandableCardTypography.current.descriptionStyle,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        padding = 4.dp
    ) {
        Column {
            startingPoints.forEach {
                val row = StartingPointData(name = it.name)
                StartingPoint(row) {
                    startingPoints.remove(row)
                    counter -= 1
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
    @Suppress("unused_parameter") modifier: Modifier = Modifier,
    showName: Boolean = true,
    showZoomToResult: Boolean = true,
    onNameChange: (String) -> Unit = {},
    onZoomRequested: () -> Unit = {}
) {
    ExpandableCard(
        title = stringResource(id = R.string.advanced_options),
        toggleable = true,
        initialExpandedState = false,
        padding = 4.dp
    ) {
        Column {
            if (showName) {
                val focusManager = LocalFocusManager.current
                AdvancedOptionsRow(name = stringResource(id = R.string.name)) {
                    var text by rememberSaveable { mutableStateOf("test trace result name") }
                    TextField(
                        value = text,
                        onValueChange = { newValue ->
                            text = newValue
                            onNameChange(newValue)
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp),
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
                ColorPicker()
            }

            if (showZoomToResult) {
                var isEnabled by rememberSaveable { mutableStateOf(false) }
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
                            if (newState) {
                                onZoomRequested()
                            }
                        },
                        modifier = Modifier
                            .semantics { contentDescription = "switch" }
                            .padding(horizontal = 4.dp),
                        enabled = true,
                        interactionSource = interactionSource
                    )
                }
            }
        }
    }
}

/**
 * A simple ColorPicker which spans the colors defined in [TraceColors.colors].
 *
 * @since 200.6.0
 */
@Composable
internal fun ColorPicker() {
    var currentSelectedColor by rememberSaveable(saver = ColorSaver.Saver()) { mutableStateOf(Color.Red) }
    var displayPicker by rememberSaveable { mutableStateOf(false) }
    Box {
        TraceColors.SpectralRing(
            currentSelectedColor,
            modifier = Modifier
                .padding(4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    displayPicker = true
                }
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = displayPicker,
                offset = DpOffset.Zero,
                onDismissRequest = { displayPicker = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TraceColors.colors.forEach {
                                Box(modifier = Modifier
                                    .size(40.dp)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(it)
                                    .clickable {
                                        currentSelectedColor = it
                                        displayPicker = false
                                    }
                                )
                            }

                        }
                    },
                    onClick = {},
                    contentPadding = PaddingValues(vertical = 0.dp, horizontal = 10.dp)
                )
            }
        }
    }
}

private object ColorSaver {
    fun Saver(): Saver<MutableState<Color>, Any> = listSaver(
        save = {
            listOf(
                it.value.component1(),
                it.value.component2(),
                it.value.component3(),
                it.value.component4()
            )
        },
        restore = {
            mutableStateOf(Color(red = it[0], green = it[1], blue = it[2], alpha = it[3]))
        }
    )
}

@Composable
private fun AdvancedOptionsRow(name: String, modifier: Modifier = Modifier, trailingTool: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ReadOnlyTextField(
            text = name,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .weight(1f)
                .align(Alignment.CenterVertically),
        )

        trailingTool()
    }
}

@Preview
@Composable
private fun AdvancedOptionsPreview() {
    AdvancedOptions()
}

@Preview
@Composable
private fun AdvancedOptionsRowPreview() {
    var isEnabled by remember { mutableStateOf(false) }
    AdvancedOptionsRow(name = stringResource(id = R.string.zoom_to_result)) {
        Switch(
            checked = isEnabled,
            onCheckedChange = { newState ->
                isEnabled = newState
            },
            modifier = Modifier
                .semantics { contentDescription = "switch" }
                .padding(horizontal = 4.dp),
            enabled = isEnabled
        )
    }
}

@Preview
@Composable
private fun TraceOptionsPreview() {
    TraceOptionsScreen(
        listOf(
            SelectableItem("Trace 1", false),
            SelectableItem("Trace 2", false),
            SelectableItem("Trace 3", false),
            SelectableItem("Trace 4", false)
        ),
        onPerformTraceButtonClicked = {},
        onAddStartingPointButtonClicked = {}
    )
}

/**
 * A data class to represent a selectable item.
 *
 * @since 200.6.0
 */
internal data class SelectableItem(val title: String, var selected: Boolean)
