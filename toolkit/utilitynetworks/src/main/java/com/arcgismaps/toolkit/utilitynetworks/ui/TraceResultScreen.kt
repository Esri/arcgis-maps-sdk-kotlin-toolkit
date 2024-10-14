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

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.TraceRun
import com.arcgismaps.toolkit.utilitynetworks.internal.util.TabRow
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityTraceFunctionOutput

/**
 * Composable that displays the trace results.
 *
 * @since 200.6.0
 */
@Composable
internal fun TraceResultScreen(
    selectedTraceRunIndex: Int,
    traceResults: List<TraceRun>,
    onSelectPreviousTraceResult: () -> Unit,
    onSelectNextTraceResult: () -> Unit,
    onBackToNewTrace: () -> Unit,
    onDeleteResult: () -> Unit,
    onZoomToResults: () -> Unit,
    onClearAllResults: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)) {

            val selectedTraceRun = traceResults[selectedTraceRunIndex]

            TabRow(onBackToNewTrace, 1)

            Row(modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TraceResultPager(
                    selectedTraceRunIndex,
                    traceResults.size,
                    onSelectPreviousTraceResult,
                    onSelectNextTraceResult
                )
            }

            TraceTitle(
                selectedTraceRun.name,
                onZoomToResults = onZoomToResults,
                onDeleteResult = onDeleteResult
            )
            LazyColumn {
                item {
                    FeatureResult(selectedTraceRun.featureResults)
                }
                item {
                    FunctionResult(selectedTraceRun.functionResults)
                }
                item {
                    ClearAllResultsButton(onClearAllResults)
                }
            }
        }
    }
}

@Composable
private fun TraceResultPager(
    selectedTraceRunIndex: Int,
    traceResultsSize: Int,
    onSelectPreviousTraceResult: () -> Unit,
    onSelectNextTraceResult: () -> Unit
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
        contentDescription = "back",
        modifier = Modifier.clickable {
            onSelectPreviousTraceResult()
        },
        tint = MaterialTheme.colorScheme.primary
    )
    Text(
        modifier = Modifier.padding(20.dp),
        text = getTraceCounterString(selectedTraceRunIndex + 1, traceResultsSize),
        style = MaterialTheme.typography.titleLarge
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = "forward",
        modifier = Modifier.clickable {
            onSelectNextTraceResult()
        },
        tint = MaterialTheme.colorScheme.primary
    )
}

private fun getTraceCounterString(currentTraceResult: Int, totalTraceResults: Int): String {
    return "Trace $currentTraceResult of $totalTraceResults"
}

@Composable
private fun TraceTitle(traceName: String, onZoomToResults: () -> Unit, onDeleteResult: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
            MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.zoom_to_result)) },
                        onClick = onZoomToResults,
                        leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = stringResource(R.string.zoom_to_trace_result)) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = onDeleteResult,
                        leadingIcon = { Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.delete_trace_result)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureResult(featureResults: List<UtilityElement>) {
    val assetGroupNames = featureResults.map { it.assetGroup.name }.distinct()

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column {
            TraceResultSection(stringResource(R.string.feature_results), value = featureResults.size.toString()) {
                Column {
                    assetGroupNames.forEach { assetGroupName ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = assetGroupName, style = MaterialTheme.typography.titleMedium)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = elementsInAssetGroup(
                                        assetGroupName,
                                        featureResults
                                    ).size.toString(), style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun elementsInAssetGroup(assetGroup: String, featureResults: List<UtilityElement>): List<UtilityElement> {
    return featureResults.filter { it.assetGroup.name == assetGroup }
}

@Composable
private fun FunctionResult(functionResults: List<UtilityTraceFunctionOutput>) {
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
                            Text(text = functionResult.function.networkAttribute.name, style = MaterialTheme.typography.titleMedium)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = functionResult.function.functionType::class.simpleName ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.Gray
                                )
                                val result = when (functionResult.result) {
                                    is Double -> formatDouble(functionResult.result as Double)
                                    else -> stringResource(R.string.not_available)
                                }
                                Text(text = result, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString() // Return as int string if decimal part is 0
    } else {
        String.format("%.5f", value) // Return as double string with 5 decimal places
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
            padding = PaddingValues(0.dp),
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
