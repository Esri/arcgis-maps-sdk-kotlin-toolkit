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
import com.arcgismaps.Guid
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state for the Trace.
 *
 * @since 200.6.0
 */
public class TraceState(
    private val arcGISMap: ArcGISMap,
    coroutineScope: CoroutineScope
) {

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>?>(null)
    public val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?> = _traceConfigurations.asStateFlow()

    private var _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)
    public val traceResult: StateFlow<UtilityElementTraceResult?> = _traceResult.asStateFlow()

    private var utilityNetwork: UtilityNetwork? = null

    init {
        coroutineScope.launch {
            arcGISMap.load().onSuccess {
                arcGISMap.utilityNetworks.forEach {
                    it.load()
                }
            }
            utilityNetwork = arcGISMap.utilityNetworks.first()
            _traceConfigurations.value = utilityNetwork?.queryNamedTraceConfigurations()?.getOrNull()
        }
    }

    public suspend fun trace() {
        // Run a trace
        val utilityNetworkDefinition = utilityNetwork?.definition
        val utilityNetworkSource =
            utilityNetworkDefinition!!.getNetworkSource("Electric Distribution Line")
        val utilityAssetGroup = utilityNetworkSource!!.getAssetGroup("Medium Voltage")
        val utilityAssetType =
            utilityAssetGroup!!.getAssetType("Underground Three Phase")
        val startingLocation = utilityNetwork?.createElementOrNull(
            utilityAssetType!!,
            Guid("0B1F4188-79FD-4DED-87C9-9E3C3F13BA77")
        )

        val utilityTraceParameters = UtilityTraceParameters(
            UtilityTraceType.Connected,
            listOf(startingLocation!!)
        )

        utilityNetwork?.trace(
            utilityTraceParameters
        )?.onSuccess {
            // Handle trace results
            _traceResult.value = it[0] as UtilityElementTraceResult
            Log.i("UtilityNetworkTraceApp", "Trace results: $it")
            Log.i(
                "UtilityNetworkTraceApp",
                "Trace result element size: ${(_traceResult.value)?.elements?.size}"
            )
        }?.onFailure {
            // Handle error
        }
    }
}
