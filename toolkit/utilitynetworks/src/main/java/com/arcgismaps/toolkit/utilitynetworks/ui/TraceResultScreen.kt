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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard

@Composable
internal fun TraceResultScreen(
    traceResults: TraceResults,
    onDeleteResult: () -> Unit,
    onZoomToResults: () -> Unit,
    onClearAllResults: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TraceTitle(traceResults.traceName, onZoomToResults = onZoomToResults, onDeleteResult = onDeleteResult)
            FunctionResult(traceResults.functionResults)
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onClearAllResults) {
                    Text("Clear all results")
                }
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
                    style = MaterialTheme.typography.titleLarge
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
                    text = { Text("Zoom To") },
                    onClick = onZoomToResults,
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = "") }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = onDeleteResult,
                    leadingIcon = { Icon(Icons.Outlined.Clear, contentDescription = "") }
                )
            }
        }
    }
}


@Composable
private fun FunctionResult(functionResults: List<FunctionResult>) {
    ExpandableCard(
        title = "${resultCountText(functionResults.size)} Function Results",
    ) {
        Column {
            functionResults.forEach { functionResult ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = functionResult.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = functionResult.value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun resultCountText(count: Int): String {
    val max = 100
    return "(${if (count > max) "$max+" else count.toString()})"
}
@Composable
@Preview
private fun TraceResultScreenPreview() {
    val traceResults = TraceResults(
        traceName = "Trace Name",
        functionResults = listOf(
            FunctionResult("Function 1", "Value 1"),
            FunctionResult("Function 2", "Value 2"),
            FunctionResult("Function 3", "Value 3"),
            FunctionResult("Function 1", "Value 1"),
            FunctionResult("Function 2", "Value 2"),
            FunctionResult("Function 3", "Value 3"),
            FunctionResult("Function 1", "Value 1"),
            FunctionResult("Function 2", "Value 2"),
            FunctionResult("Function 3", "Value 3"),
            FunctionResult("Function 1", "Value 1"),
            FunctionResult("Function 2", "Value 2"),
            FunctionResult("Function 3", "Value 3"),


        )
    )
    TraceResultScreen(traceResults, onClearAllResults = {}, onDeleteResult = {}, onZoomToResults = {})
}

internal data class TraceResults(val traceName: String, val functionResults: List<FunctionResult>)
internal data class FunctionResult(val name: String, val value: String)
