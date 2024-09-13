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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
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
public sealed interface TraceState {

    public val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>>

    public val traceResult: StateFlow<UtilityElementTraceResult?>
    public val addStartingPointMode: StateFlow<AddStartingPointMode>
    public val selectedGeoElementsAsStartingPoints: StateFlow<List<GeoElement>>
    public val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?>
    public fun addStartingPoint(mapPoint: Point)
    public fun updateAddStartPointMode(status: AddStartingPointMode)
    public fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration)
    public suspend fun trace()
}

/**
 * Default implementation for [TraceState].
 *
 * @since 200.6.0
 */
private class TraceStateImpl(
    val arcGISMap: ArcGISMap,
    val coroutineScope: CoroutineScope,
    val graphicsOverlay: GraphicsOverlay,
    val mapViewProxy: MapViewProxy
) : TraceState {

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>>(emptyList())
    override val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>> = _traceConfigurations.asStateFlow()

    private var _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)
    override val traceResult: StateFlow<UtilityElementTraceResult?> = _traceResult.asStateFlow()

    private var _addStartingPointMode = MutableStateFlow<AddStartingPointMode>(AddStartingPointMode.None)
    override val addStartingPointMode: StateFlow<AddStartingPointMode> = _addStartingPointMode.asStateFlow()

    private var _selectedTraceConfiguration: MutableState<UtilityNamedTraceConfiguration?> = mutableStateOf(null)
    override val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?> = _selectedTraceConfiguration

    private val _selectedGeoElementsAsStartingPoints: MutableStateFlow<MutableList<GeoElement>> =
        MutableStateFlow(
            mutableListOf()
        )
    override val selectedGeoElementsAsStartingPoints: StateFlow<List<GeoElement>> =
        _selectedGeoElementsAsStartingPoints.asStateFlow()

    private lateinit var tapPoint: Point
    private var screenPoint: ScreenCoordinate? = null

    private var _utilityNetwork: UtilityNetwork? = null
    private val utilityNetwork: UtilityNetwork
        get() = _utilityNetwork ?: throw IllegalStateException("utility network cannot be null")

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
//            if (arcGISMap.utilityNetworks.isEmpty()) {
                // //Handle error
//                }

            _utilityNetwork = arcGISMap.utilityNetworks.first()
            _traceConfigurations.value = utilityNetwork.queryNamedTraceConfigurations().getOrThrow()
        }
    }

    override fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
    }
    override suspend fun trace() {
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

        utilityNetwork.trace(
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

    override fun addStartingPoint(mapPoint: Point) {
        if (_addStartingPointMode.value is AddStartingPointMode.Started) {
            tapPoint = mapPoint
            screenPoint = mapViewProxy.locationToScreenOrNull(mapPoint)
            screenPoint?.let { identifyFeatures(it, coroutineScope) }
        }
    }

    override fun updateAddStartPointMode(status: AddStartingPointMode) {
        _addStartingPointMode.value = status
    }

    private fun identifyFeatures(screenCoordinate: ScreenCoordinate, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val result = mapViewProxy.identifyLayers(
                screenCoordinate = screenCoordinate,
                tolerance = 10.dp
            )
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    identifyLayerResultList.forEach { identifyLayerResult ->
                        identifyLayerResult.geoElements.forEach { geoElement ->
                            _selectedGeoElementsAsStartingPoints.value.add(geoElement)
                        }
                    }
                    addTapLocationToGraphicsOverlay(tapPoint)
                    _addStartingPointMode.value = AddStartingPointMode.Stopped
                }
            }
        }
    }

    private fun addTapLocationToGraphicsOverlay(mapPoint: Point) {
        graphicsOverlay.graphics.add(
            Graphic(
                geometry = mapPoint,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20.0f)
            )
        )
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

/**
 * Factory function for the creating TraceState.
 *
 * @param arcGISMap the map containing the UtilityNetworks
 * @param coroutineScope scope for [TraceState] that it can use to load the [arcGISMap] and the
 *        UtilityNetworks
 * @param graphicsOverlay The graphics overlay to show tap location
 *
 * @since 200.6.0
 */
public fun TraceState(
    arcGISMap: ArcGISMap,
    coroutineScope: CoroutineScope,
    graphicsOverlay: GraphicsOverlay,
    mapViewProxy: MapViewProxy
): TraceState =
    TraceStateImpl(arcGISMap, coroutineScope, graphicsOverlay, mapViewProxy)
