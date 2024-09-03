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
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.Guid
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state for the Trace.
 *
 * @since 200.6.0
 */
public sealed interface TraceState {

    public val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?>

    public val selectedGeoElementsAsStartingPoints: StateFlow<List<GeoElement>>

    public val traceResult: StateFlow<UtilityElementTraceResult?>

    public fun setStartingPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent)

    public fun removeStartingPoint(position: Int)

    public fun trace()
}

/**
 * Default implementation for [TraceState].
 *
 * @since 200.6.0
 */
private class TraceStateImpl(
    var arcGISMap: ArcGISMap,
    var coroutineScope: CoroutineScope,
    var mapViewProxy:  MapViewProxy,
    var graphicsOverlay: GraphicsOverlay,
    var onAddStartingPointModeChangedListener: () -> Unit
) : TraceState {

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>?>(null)
    override val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?> = _traceConfigurations

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation.asStateFlow()

    private val _selectedGeoElementsAsStartingPoints: MutableStateFlow<MutableList<GeoElement>> =
        MutableStateFlow(
            mutableListOf()
        )
    override val selectedGeoElementsAsStartingPoints: StateFlow<List<GeoElement>> = _selectedGeoElementsAsStartingPoints.asStateFlow()

    private var _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)
    override val traceResult: StateFlow<UtilityElementTraceResult?> = _traceResult

    private var currentIdentifyJob: Job? = null

    private lateinit var utilityNetwork: UtilityNetwork

    init {
        coroutineScope.launch {
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

            utilityNetwork = arcGISMap.utilityNetworks.first()
            _traceConfigurations.value = utilityNetwork.queryNamedTraceConfigurations().getOrNull()
        }
    }

    override fun setStartingPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        _tapLocation.value = singleTapConfirmedEvent.mapPoint

        graphicsOverlay.graphics.clear()
        graphicsOverlay.graphics.add(
            Graphic(
                geometry = singleTapConfirmedEvent.mapPoint,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    override fun removeStartingPoint(position: Int) {
        _selectedGeoElementsAsStartingPoints.value.removeAt(position)
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The
     * identified geoelements are set to [_selectedGeoElementsAsStartingPoints].
     *
     * @since 200.6.0
     */
    private fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = coroutineScope.launch {
            val result = mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 1.dp
            )
            result.onSuccess { identifyLayerResultList ->
                identifyLayerResultList.forEach { identifyLayerResult ->
                    identifyLayerResult.geoElements.forEach { geoElement ->
                        _selectedGeoElementsAsStartingPoints.value.add(geoElement)
                    }
                }
            }
        }
    }

    override fun trace() {
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

            val traceResultList = utilityNetwork.trace(
                utilityTraceParameters
            ).onSuccess {
                // Handle trace results
                _traceResult.value = it[0] as UtilityElementTraceResult
                Log.i("UtilityNetworkTraceApp", "Trace results: $it")
                Log.i(
                    "UtilityNetworkTraceApp",
                    "Trace result element size: ${(_traceResult.value)?.elements?.size}"
                )
            }.onFailure {
                // Handle error
            }
        }
    }


    fun clearTapLocationAndGraphic() {
        _tapLocation.value = null
        graphicsOverlay.graphics.clear()
    }
}

/**
 * Factory function for the creating TraceState.
 *
 * @param arcGISMap the floor aware geoModel that drives the [FloorFilter]
 * @param coroutineScope scope for [TraceState] that it can use to load the [arcGISMap] and the
 *        UtilityNetworks
 * @param mapViewProxy the [MapViewProxy] associated with the composable MapView
 * @param graphicsOverlay the [GraphicsOverlay] show generated starting point and trace graphics.
 * @param onAddStartingPointModeChangedListener a lambda to facilitate changing the state of bottomsheet
 *        whenever the user either puts the trace tool in AddStartingPointMode or gets out of it.
 *
 * @since 200.6.0
 */
public fun TraceState(
    arcGISMap: ArcGISMap,
    coroutineScope: CoroutineScope,
    mapViewProxy: MapViewProxy,
    graphicsOverlay: GraphicsOverlay,
    onAddStartingPointModeChangedListener: () -> Unit = { }
): TraceState =
    TraceStateImpl(arcGISMap, coroutineScope, mapViewProxy, graphicsOverlay, onAddStartingPointModeChangedListener)