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
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
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

    /**
     * The named trace configurations of the Utility Network
     *
     * @since 200.6.0
     */
    public val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?> = _traceConfigurations.asStateFlow()

    private val _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)

    /**
     * The results of running the  trace operation on the Utility Network from the selected
     * starting point(s).
     *
     * @since 200.6.0
     */
    public val traceResult: StateFlow<UtilityElementTraceResult?> = _traceResult.asStateFlow()

    private val _addStartingPointMode = MutableStateFlow<AddStartingPointMode>(AddStartingPointMode.None)

    /**
     * Governs taps on the map. When the mode is [AddStartingPointMode.Started] taps will identify starting points
     * and pass underlying Features to this object.
     *
     * @since 200.6.0
     * @see AddStartingPointMode]
     */
    public val addStartingPointMode: StateFlow<AddStartingPointMode> = _addStartingPointMode.asStateFlow()

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

    /**
     * TBD
     */
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

    /**
     * A single tap handler to identify starting points on the map. Call this method
     * from [com.arcgismaps.toolkit.geoviewcompose.GeoViewProxy.identify].
     *
     * @param point the event raised by a single tap on the map
     * @since 200.6.0
     */
    public fun addStartingPoint(point: SingleTapConfirmedEvent) {
        if (_addStartingPointMode.value is AddStartingPointMode.Started) {
            // TODO: identify
            // TODO: add point to graphic overlay
            _addStartingPointMode.value = AddStartingPointMode.Stopped
        }
    }

    /**
     * Set the mode of the state object to activate or deactivate the identification of
     * `Features` in [com.arcgismaps.toolkit.geoviewcompose.MapViewEventHandler] response
     * to single tap events.
     *
     * @param status the updated mode
     * @since 200.6.0
     */
    public fun updateAddStartPointMode(status: AddStartingPointMode) {
        _addStartingPointMode.value = status
    }
}

/**
 * Represents the mode when adding starting points.
 *
 * @since 200.6.0
 */
public sealed class AddStartingPointMode {
    /**
     * Utility Network Trace tool is in add starting points mode.
     * @since 200.6.0
     */
    public data object Started : AddStartingPointMode()

    /**
     * Utility Network Trace tool is not adding starting points.
     *
     * @since 200.6.0
     */
    public data object Stopped : AddStartingPointMode()

    /**
     * Utility Network Trace is neither started nor stopped.
     *
     * @since 200.6.0
     */
    public data object None : AddStartingPointMode()
}
