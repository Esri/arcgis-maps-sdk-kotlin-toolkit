/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arcgismaps.toolkit.utilitynetworks.ui.material3.Slider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint
import com.arcgismaps.toolkit.utilitynetworks.internal.util.Title
import com.arcgismaps.toolkit.utilitynetworks.internal.util.UpButton
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTerminal

/**
 * A composable screen that shows the details of a starting point for a trace.
 *
 * @since 200.6.0
 */
@Composable
internal fun StartingPointDetailsScreen(
    startingPoint: StartingPoint,
    onZoomTo: () -> Unit,
    onDelete: () -> Unit,
    onFractionChanged: (StartingPoint, Float) -> Unit,
    onTerminalSelected: (UtilityTerminal) -> Unit,
    onBackPressed: () -> Unit
) {
    BackHandler {
        onBackPressed()
    }

    Column {
        UpButton(stringResource(id = R.string.starting_points), onBackPressed)

        Title(startingPoint.name, onZoomTo, onDelete)

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (startingPoint.utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Edge) {
                item {
                    FractionAlongEdgeSlider(startingPoint, onFractionChanged)
                }
            } else if (startingPoint.utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Junction
                && startingPoint.utilityElement.terminal != null
                && !startingPoint.utilityElement.assetType.terminalConfiguration?.terminals.isNullOrEmpty()
            ) {
                item {
                    TerminalConfiguration(startingPoint, onTerminalSelected)
                }
            }
            item {
                Attributes(startingPoint)
            }
        }
    }
}

@Composable
private fun FractionAlongEdgeSlider(
    startingPoint: StartingPoint,
    onFractionChanged: (StartingPoint, Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(startingPoint.utilityElement.fractionAlongEdge.toFloat()) }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.fraction_along_edge),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Slider(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    onFractionChanged(startingPoint, newValue)
                }
            )
        }
    }
}

@Composable
private fun Attributes(startingPoint: StartingPoint) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.attributes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            val attributes = startingPoint.feature.attributes.toSortedMap().toList()
            attributes.forEachIndexed { index, attribute ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = attribute.first,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = attribute.second as? String ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < attributes.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun TerminalConfiguration(
    startingPoint: StartingPoint,
    onTerminalSelected: (UtilityTerminal) -> Unit
) {
    var showDropdown by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedTerminalName by rememberSaveable {
        mutableStateOf(startingPoint.utilityElement.terminal?.name)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.terminal_configuration),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(5.dp)
                )
                .background(color = MaterialTheme.colorScheme.background)
                .clickable {
                    showDropdown = !showDropdown
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReadOnlyTextField(
                text = selectedTerminalName ?: "",
                modifier = Modifier.clickable {
                    showDropdown = !showDropdown
                },
                trailingIcon = {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(id = R.string.edit_terminal_configuration),
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                            DropdownMenu(
                                expanded = showDropdown,
                                offset = DpOffset(16.dp, 0.dp),
                                onDismissRequest = { showDropdown = false }) {
                                startingPoint.utilityElement.assetType.terminalConfiguration?.terminals?.forEach { utilityTerminal ->
                                    DropdownMenuItem(
                                        text = {
                                            ReadOnlyTextField(
                                                text = utilityTerminal.name,
                                                leadingIcon = if (utilityTerminal.name == selectedTerminalName) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Done,
                                                            contentDescription = stringResource(id = R.string.selected),
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else {
                                                    null
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedTerminalName = utilityTerminal.name
                                            onTerminalSelected(utilityTerminal)
                                            showDropdown = false
                                        },
                                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
