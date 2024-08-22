/*
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

package com.arcgismaps.toolkit.utilitynetworks

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.utilitynetworks.ui.SelectableItem
import com.arcgismaps.toolkit.utilitynetworks.ui.TraceOptions
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.Guid
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A composable UI component to set up and run a [com.arcgismaps.utilitynetworks.UtilityNetwork.trace]
 * on a [com.arcgismaps.toolkit.geoviewcompose.MapView].
 *
 * @param utilityNetwork a [UtilityNetwork]
 * @since 200.6.0
 */
@Composable
public fun Trace(
    arcGISMap: ArcGISMap,
//    graphicsOverlay: GraphicsOverlay,
//    mapPoint: Point,
    @Suppress("unused_parameter")
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(arcGISMap) {
        arcGISMap.load().onSuccess {
            arcGISMap.utilityNetworks.forEach {
                it.load().onFailure {
                    // Handle error
                }.onSuccess {

                }
            }
        }.onFailure {
            // Handle error
        }
        if (arcGISMap.utilityNetworks.isEmpty()) {
            // Handle error
        }
    }

    val utilityNetwork = arcGISMap.utilityNetworks.first()
    val availableTraces = remember { mutableStateListOf<UtilityNamedTraceConfiguration>() }

    LaunchedEffect(key1 = null) {
        val result = utilityNetwork.queryNamedTraceConfigurations().getOrNull()
        result?.forEach {
            availableTraces.add(it)
        }
    }
    TraceOptions(
        traceTypes = availableTraces.map { SelectableItem(it.name, false) },
        onPerformTrace = { trace(coroutineScope, utilityNetwork) }
    )
}

private fun trace(coroutineScope: CoroutineScope, utilityNetwork: UtilityNetwork) {
    coroutineScope.launch {
        // Run a trace
        val utilityNetworkDefinition = utilityNetwork.definition
        val utilityNetworkSource =
            utilityNetworkDefinition!!.getNetworkSource("Electric Distribution Line")
        val utilityAssetGroup = utilityNetworkSource!!.getAssetGroup("Medium Voltage")
        val utilityAssetType =
            utilityAssetGroup!!.getAssetType("Underground Three Phase")
        val startingLocation = utilityNetwork.createElementOrNull(
            utilityAssetType!!,
            Guid("0B1F4188-79FD-4DED-87C9-9E3C3F13BA77")
        )

        val utilityTraceParameters = UtilityTraceParameters(
            UtilityTraceType.Connected,
            listOf(startingLocation!!)
        )

        val traceResults = utilityNetwork.trace(
            utilityTraceParameters
        ).onSuccess {
            // Handle trace results
            val traceResult = it[0]
            Log.i("UtilityNetworkTraceApp", "Trace results: $it")
            Log.i(
                "UtilityNetworkTraceApp",
                "Trace result element size: ${(traceResult as UtilityElementTraceResult).elements.size}"
            )
        }.onFailure {
            // Handle error
        }
    }
}
