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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint
import com.arcgismaps.toolkit.utilitynetworks.internal.util.Title
import com.arcgismaps.toolkit.utilitynetworks.ui.material3.Slider
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType

/**
 * A composable screen that shows the details of a starting point for a trace.
 *
 * @since 200.6.0
 */
@Composable
internal fun StartingPointDetailsScreen(startingPoint: StartingPoint,
                                        onZoomToResults: () -> Unit,
                                        onClearAllResults: () -> Unit,
                                        onFractionChanged: (StartingPoint, Float) -> Unit,
                                        onBackPressed: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top) {
            Row(
                modifier = Modifier.clickable { onBackPressed() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "back"
                )
                Text(
                    text = stringResource(id = R.string.starting_points),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Title(startingPoint.name, onZoomToResults, onClearAllResults)

            LazyColumn(
                modifier = Modifier
                    .padding(10.dp, 3.dp),
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
                        TerminalConfiguration(startingPoint)
                    }
                }
                item {
                    Attributes(startingPoint)
                }
            }
        }
    }
}

@Composable
private fun FractionAlongEdgeSlider(startingPoint: StartingPoint, onFractionChanged: (StartingPoint, Float) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(startingPoint.utilityElement.fractionAlongEdge.toFloat()) }
    Column {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp))
        Text(text = stringResource(id = R.string.fraction_along_edge),
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp))
        Box (modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(color = MaterialTheme.colorScheme.background)) {
            Slider(
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
    Column {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
        Text(
            text = stringResource(id = R.string.attributes),
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            val attributes = startingPoint.feature.attributes.toSortedMap().toList()
            attributes.forEachIndexed { index, attribute ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(color = MaterialTheme.colorScheme.background),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.padding(start = 5.dp),
                        text = attribute.first,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.padding(end = 5.dp),
                        text = attribute.second as? String ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < attributes.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun TerminalConfiguration(startingPoint: StartingPoint) {
    var showDropdown by rememberSaveable {
        mutableStateOf(false)
    }
    Column {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp))
        Text(text = stringResource(id = R.string.terminal_configuration),
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp))
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = showDropdown,
                offset = DpOffset.Zero,
                onDismissRequest = { showDropdown = false }) {
                startingPoint.utilityElement.assetType.terminalConfiguration?.terminals?.forEachIndexed { index, utilityTerminal ->
                    DropdownMenuItem(
                        text = {
                            ReadOnlyTextField(
                                text = utilityTerminal.name, leadingIcon = if (utilityTerminal.name == selectedConfigName) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = "Done icon",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        },
                        onClick = {
                            onTraceSelected(index)
                            showDropdown = false
                        },
                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 10.dp)
                    )
                }
            }
        }
    }
}
