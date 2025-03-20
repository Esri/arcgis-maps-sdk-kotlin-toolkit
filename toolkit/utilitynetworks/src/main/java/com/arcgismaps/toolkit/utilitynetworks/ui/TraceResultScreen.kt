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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.rememberExpandableCardState
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.TraceRun
import com.arcgismaps.toolkit.utilitynetworks.internal.util.AdvancedOptionsRow
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ColorPicker
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ExpandableCardWithLabel
import com.arcgismaps.toolkit.utilitynetworks.internal.util.Title
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
    onFeatureGroupSelected: (String) -> Unit,
    onDeleteResult: () -> Unit,
    onZoomToResults: () -> Unit,
    onColorChanged: (Color) -> Unit,
    onClearAllResults: () -> Unit
) {
    BackHandler {
        onBackToNewTrace()
    }

    if (traceResults.isNotEmpty()) {
        Column {

            val selectedTraceRun = traceResults[selectedTraceRunIndex]
            var selectedColor by remember(selectedTraceRunIndex) {
                mutableStateOf(
                    selectedTraceRun.resultGraphicColor
                )
            }

            if (traceResults.size > 1) {
                TraceResultPager(
                    selectedTraceRunIndex,
                    traceResults.size,
                    onSelectPreviousTraceResult,
                    onSelectNextTraceResult
                )
            }

            Title(
                selectedTraceRun.name,
                onZoomTo = onZoomToResults,
                onDelete = onDeleteResult,
                showZoomToOption = selectedTraceRun.geometryTraceResult != null
            )
            LazyColumn(
                modifier = Modifier.weight(weight = 1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FeatureResult(selectedTraceRun.featureResults, onFeatureGroupSelected)
                }
                item {
                    FunctionResult(selectedTraceRun.functionResults)
                }
                item {
                    AdvancedOptions(
                        selectedColor = selectedColor,
                        onColorChanged = {
                            selectedColor = it
                            onColorChanged(it)
                        }
                    )
                }
            }
            ClearAllResultsButton(onClearAllResults)
        }
    } else {
        Column {
            // Don't show anything if there are no trace results
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = stringResource(id = R.string.select_previous_result),
            modifier = Modifier.clickable {
                onSelectPreviousTraceResult()
            },
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            modifier = Modifier.padding(horizontal = 20.dp),
            text = getTraceCounterString(selectedTraceRunIndex + 1, traceResultsSize),
            style = MaterialTheme.typography.titleLarge
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = stringResource(id = R.string.select_next_result),
            modifier = Modifier.clickable {
                onSelectNextTraceResult()
            },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun getTraceCounterString(currentTraceResult: Int, totalTraceResults: Int): String {
    return "Trace $currentTraceResult of $totalTraceResults"
}

@Composable
private fun FeatureResult(featureResults: List<UtilityElement>, onFeatureAssetGroupSelected: (String) -> Unit) {
    val assetGroupNames = featureResults
        .map { it.assetGroup.name }
        .filter { it.isNotEmpty() }
        .distinct()

    ExpandableCardWithLabel(
        labelText = stringResource(R.string.feature_results),
        contentTitle = featureResults.size.toString()
    ) {
        Column {
            assetGroupNames.forEach { assetGroupName ->
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        .clickable { onFeatureAssetGroupSelected(assetGroupName) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = assetGroupName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = elementsInAssetGroup(
                                assetGroupName,
                                featureResults
                            ).size.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
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
    ExpandableCardWithLabel(
        labelText = stringResource(R.string.function_results),
        contentTitle = functionResults.size.toString()
    ) {
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
                    Text(
                        text = functionResult.function.networkAttribute.name,
                        style = MaterialTheme.typography.titleMedium
                    )
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

/**
 * A composable used to display the advanced options
 *
 * @since 200.6.0
 */
@Composable
internal fun AdvancedOptions(
    @Suppress("unused_parameter") modifier: Modifier = Modifier,
    selectedColor: Color,
    onColorChanged: (Color) -> Unit = {},
) {
    ExpandableCard(
        title = stringResource(id = R.string.advanced_options),
        toggleable = true,
        expandableCardState = rememberExpandableCardState(false),
        padding = PaddingValues()
    ) {
        Column {
            // Color picker
            AdvancedOptionsRow(name = stringResource(id = R.string.color)) {
                ColorPicker(selectedColor, onColorChanged)
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
private fun ClearAllResultsButton(onClearAllResults: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Button(onClick = onClearAllResults) {
            Text(stringResource(R.string.clear_all_results))
        }
    }
}
