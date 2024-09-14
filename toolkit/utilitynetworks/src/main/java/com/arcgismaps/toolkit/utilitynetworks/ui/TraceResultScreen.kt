/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.R

/**
 * Composable that displays the trace results.
 *
 * @since 200.6.0
 */
@Composable
internal fun TraceResultScreen(
    traceResults: TraceResults,
    onDeleteResult: () -> Unit,
    onZoomToResults: () -> Unit,
    onClearAllResults: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            item {
                TraceTitle(traceResults.traceName, onZoomToResults = onZoomToResults, onDeleteResult = onDeleteResult)
            }
            item {
                FunctionResult(traceResults.functionResults)
            }
            item {
                ClearAllResultsButton(onClearAllResults)
            }
        }
    }
}

@Composable
private fun TraceTitle(traceName: String, onZoomToResults: () -> Unit, onDeleteResult: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box {
            Row(modifier = Modifier.clickable {
                expanded = !expanded
            }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = traceName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = ""
                )
            }
            DropdownMenu(
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.zoom_to_result)) },
                    onClick = onZoomToResults,
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = "Zoom to trace result") }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = onDeleteResult,
                    leadingIcon = { Icon(Icons.Outlined.Clear, contentDescription = "Delete trace result") }
                )
            }
        }
    }
}


@Composable
private fun FunctionResult(functionResults: List<FunctionResult>) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column {
            TraceResultSection(stringResource(R.string.function_results), value = functionResults.size.toString()) {
                Column {
                    functionResults.forEach { functionResult ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = functionResult.name, style = MaterialTheme.typography.titleMedium)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = functionResult.functionType,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.Gray
                                )
                                Text(text = functionResult.value, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TraceResultSection(title: String, value: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            title,
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        ExpandableCard(
            initialExpandedState = false,
            title = value,
            padding = 0.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ClearAllResultsButton(onClearAllResults: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Button(onClick = onClearAllResults) {
            Text(stringResource(R.string.clear_all_results))
        }
    }
}

@Composable
@Preview
private fun TraceResultScreenPreview() {
    val traceResults = TraceResults(
        traceName = "Trace Name",
        functionResults = listOf(
            FunctionResult("Function 1", "1", "Add"),
            FunctionResult("Function 2", " 2", "Average"),
            FunctionResult("Function 3", " 3", "Count"),
            FunctionResult("Function 1", "1", "Max"),
            FunctionResult("Function 2", "2", "Min"),
            FunctionResult("Function 3", "3", "Subtract"),
            )
    )
    TraceResultScreen(traceResults, onClearAllResults = {}, onDeleteResult = {}, onZoomToResults = {})
}
/**
 * Data class to hold the trace results.
 *
 * @param traceName The name of the trace.
 * @param functionResults The list of function results.
 * @since 200.6.0
 */
internal data class TraceResults(val traceName: String, val functionResults: List<FunctionResult>)

/**
 * Data class to hold the function result.
 *
 * @param name The name of the function result.
 * @param value The value of the function result.
 * @param functionType The type of the function result.
 * @since 200.6.0
 */
internal data class FunctionResult(val name: String, val value: String, val functionType: String)
