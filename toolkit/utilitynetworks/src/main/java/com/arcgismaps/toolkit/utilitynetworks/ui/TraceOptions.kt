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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
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
internal fun TraceOptions(configurations: List<SelectableItem>, onPerformTrace: () -> Unit) {
    val traceConfigurations = remember { mutableStateListOf<SelectableItem>() }
    traceConfigurations.addAll(configurations)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                TraceConfiguration(
                    traceConfigurations
                ) { index ->
                    traceConfigurations[index] =
                        traceConfigurations[index].copy(selected = !traceConfigurations[index].selected)
                    traceConfigurations.forEachIndexed { i, _ ->
                        if (i != index) {
                            traceConfigurations[i] =
                                traceConfigurations[i].copy(selected = false)
                        }
                    }
                }

            }
            item {
                StartingPointsEditor()
            }
            item {
                Button(onClick = { onPerformTrace() }) {
                    Text(stringResource(id = R.string.trace))
                }
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
private fun TraceConfiguration(utilityTraces: List<SelectableItem>, onTraceSelected: (Int) -> Unit) {
    ExpandableCard(
        title = stringResource(id = R.string.trace_configuration)
    ) {
        Column {
            utilityTraces.forEachIndexed { index, item ->
                FilterChip(
                    onClick = { onTraceSelected(index) },
                    label = {
                        Text(item.title)
                    },
                    selected = item.selected,
                    leadingIcon = if (item.selected) {
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
private fun StartingPointsEditor() {
    val startingPoints = remember { mutableStateListOf<String>() }
    var counter by remember { mutableIntStateOf(startingPoints.size) }
    ExpandableCard(
        title = "${stringResource(id = R.string.starting_points)} (${counter})"
    ) {
        Column {
            ElevatedButton(
                onClick = {
                    startingPoints.add("Point ${counter++}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.add_starting_point))
            }
            startingPoints.forEach {
                ExpandableCard(title = it, toggleable = false)
            }
        }

    }
}

@Preview
@Composable
private fun TraceOptionsPreview() {
    TraceOptions(
        listOf(
            SelectableItem("Trace 1", false),
            SelectableItem("Trace 2", false),
            SelectableItem("Trace 3", false),
            SelectableItem("Trace 4", false)
        )
    ) {}
}

/**
 * A data class to represent a selectable item.
 *
 * @since 200.6.0
 */
internal data class SelectableItem(val title: String, var selected: Boolean)