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
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the state for the Trace.
 *
 * @since 200.6.0
 */
public class TraceState(
    private val arcGISMap: ArcGISMap,
    private val graphicsOverlay: GraphicsOverlay,
    private val mapViewProxy: MapViewProxy
) {

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>?>(null)

    private val _initializationStatus = MutableStateFlow<InitializationStatus>(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.6.0
     */
    public val initializationStatus: StateFlow<InitializationStatus> = _initializationStatus

    /**
     * The named trace configurations of the Utility Network
     *
     * @since 200.6.0
     */
    internal val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?> = _traceConfigurations.asStateFlow()

    private val _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)

    /**
     * The results of running the  trace operation on the Utility Network from the selected
     * starting point(s).
     *
     * @since 200.6.0
     */
    internal val traceResult: StateFlow<UtilityElementTraceResult?> = _traceResult.asStateFlow()

    private val _addStartingPointMode = MutableStateFlow<AddStartingPointMode>(AddStartingPointMode.None)

    /**
     * Governs taps on the map. When the mode is [AddStartingPointMode.Started] taps will identify starting points
     * and pass underlying Features to this object.
     *
     * @since 200.6.0
     * @see AddStartingPointMode]
     */
    public val addStartingPointMode: StateFlow<AddStartingPointMode> = _addStartingPointMode.asStateFlow()

    private val _selectedGeoElementsAsStartingPoints: MutableStateFlow<MutableList<GeoElement>> =
        MutableStateFlow(
            mutableListOf()
        )

    /**
     * The selected `GeoElements` on the map that are used as starting points for the trace.
     *
     * @since 200.6.0
     */
    internal val selectedGeoElementsAsStartingPoints: StateFlow<List<GeoElement>> =
        _selectedGeoElementsAsStartingPoints.asStateFlow()

    private var utilityNetwork: UtilityNetwork? = null

    /**
     * Initializes the state object by loading the map, the Utility Networks contained in the map
     * and its trace configurations.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.6.0
     */
    public suspend fun initialize(): Result<Unit> {
        _initializationStatus.value = InitializationStatus.Initializing
        var result = Result.success(Unit)
        arcGISMap.load().onSuccess {
            arcGISMap.utilityNetworks.forEach { utilityNetwork ->
                utilityNetwork.load().onFailure { error ->
                    result = Result.failure(error)
                    _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
                }
            }
        }.onFailure {
            result = Result.failure(it)
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
        }
        _initializationStatus.value = InitializationStatus.Initialized
        utilityNetwork = arcGISMap.utilityNetworks.first()
        _traceConfigurations.value = utilityNetwork?.queryNamedTraceConfigurations()?.getOrNull()
        return result
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
     * from [com.arcgismaps.toolkit.geoviewcompose.MapView] onSingleTapConfirmed lambda.
     *
     * @param mapPoint the point on the map user tapped on to identify starting points
     * @since 200.6.0
     */
    public suspend fun addStartingPoint(mapPoint: Point) {
        if (_addStartingPointMode.value is AddStartingPointMode.Started) {
            val screenPoint = mapViewProxy.locationToScreenOrNull(mapPoint)
            screenPoint?.let { identifyFeatures(mapPoint, it) }
        }
    }

    private suspend fun identifyFeatures(mapPoint: Point, screenCoordinate: ScreenCoordinate) {
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
                addTapLocationToGraphicsOverlay(mapPoint)
                _addStartingPointMode.value = AddStartingPointMode.Stopped
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

    /**
     * Set the mode of the state object to activate or deactivate the identification of
     * `GeoElements` in [com.arcgismaps.toolkit.geoviewcompose.MapView] onSingleTapConfirmed response
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
 * Represents the status of the initialization of the state object.
 *
 * @since 200.6.0
 */
public sealed class InitializationStatus {
    /**
     * The state object is initialized and ready to use.
     *
     * @since 200.6.0
     */
    public data object Initialized : InitializationStatus()

    /**
     * The state object is initializing.
     *
     * @since 200.6.0
     */
    public data object Initializing : InitializationStatus()

    /**
     * The state object is not initialized.
     *
     * @since 200.6.0
     */
    public data object NotInitialized : InitializationStatus()

    /**
     * The state object failed to initialize.
     *
     * @since 200.6.0
     */
    public data class FailedToInitialize(val error: Throwable) : InitializationStatus()
}

/**
 * Represents the mode when adding starting points.
 *
 * @since 200.6.0
 */
public sealed class AddStartingPointMode {
    /**
     * Utility Network Trace tool is in add starting points mode.
     *
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