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

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityFunctionTraceResult
import com.arcgismaps.utilitynetworks.UtilityGeometryTraceResult
import com.arcgismaps.utilitynetworks.UtilityMinimumStartingLocations
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceFunctionOutput
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceResult
import kotlinx.coroutines.CancellationException
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
    coroutineScope: CoroutineScope,
    private val graphicsOverlay: GraphicsOverlay,
    private val mapViewProxy: MapViewProxy
) {

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>>(emptyList())

    /**
     * The named trace configurations of the Utility Network
     *
     * @since 200.6.0
     */
    internal val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>> = _traceConfigurations.asStateFlow()

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

    private var _selectedTraceConfiguration: MutableState<UtilityNamedTraceConfiguration?> = mutableStateOf(null)

    /**
     * The selected trace configuration for the TraceParameters that define the trace.
     *
     * @since 200.6.0
     */
    public val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?> = _selectedTraceConfiguration

    private val _startingPoints: SnapshotStateList<StartingPoint> = mutableStateListOf()

    internal val startingPoints: List<StartingPoint> = _startingPoints

    private var _utilityNetwork: UtilityNetwork? = null
    private val utilityNetwork: UtilityNetwork
        get() = _utilityNetwork ?: throw IllegalStateException("utility network cannot be null")


    init {
        coroutineScope.launch {
            arcGISMap.load().getOrThrow()
            arcGISMap.utilityNetworks.forEach {
                it.load().getOrThrow()
            }
            _utilityNetwork = arcGISMap.utilityNetworks.first()
            _traceConfigurations.value = utilityNetwork.queryNamedTraceConfigurations().getOrThrow()
        }
    }

    internal fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
    }

    /**
     * TBD
     */
    public suspend fun trace() {
        // Run a trace
        val traceConfiguration = selectedTraceConfiguration.value ?: return // this should be handled in UI

        if (startingPoints.isEmpty() && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.One) {
            // TODO: Handle error
            return
        }

        if (startingPoints.size < 2 && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.Many) {
            // TODO: Handle error
            return
        }

        val utilityTraceParameters = UtilityTraceParameters(traceConfiguration, startingPoints.map { it.utilityElement })

        var traceResults: List<UtilityTraceResult> = emptyList()

        utilityNetwork.trace(
            utilityTraceParameters
        ).onSuccess {
            // Handle trace results
            _traceResult.value = it[0] as UtilityElementTraceResult
            traceResults = it
            Log.i("UtilityNetworkTraceApp", "Trace results: $it")
            Log.i(
                "UtilityNetworkTraceApp",
                "Trace result element size: ${(_traceResult.value)?.elements?.size}"
            )
        }.onFailure {
            // TODO: Handle error
            Log.i("UtilityNetworkTraceApp", "Trace failed: $it")
        }

        var currentTraceElementResults : List<UtilityElement> = emptyList()
        val currentTraceFunctionResults : MutableList<UtilityTraceFunctionOutput> = mutableListOf()
        val currentTraceGraphics : MutableList<Graphic> = mutableListOf()


        for (result in traceResults) {
            Log.i("UtilityNetworkTraceApp", "Trace result: $result")
            when (result) {
                is UtilityElementTraceResult -> {
                    currentTraceElementResults = result.elements
                }

                is UtilityFunctionTraceResult -> {
                    result.functionOutputs.forEach {
                        currentTraceFunctionResults.add(it)
                    }
                }

                is UtilityGeometryTraceResult -> {
                    result.polygon?.let { polygon ->
                        val graphic = createGraphic(polygon, SimpleLineSymbolStyle.Solid, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    result.polyline?.let { polyline ->
                        val graphic = createGraphic(polyline, SimpleLineSymbolStyle.Dash, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    result.multipoint?.let { multipoint ->
                        val graphic = createGraphic(multipoint, SimpleLineSymbolStyle.Dot, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    currentTraceGraphics.map { it.isSelected = true }
                }
            }
        }
    }

    private fun createGraphic(geometry: Geometry, style: SimpleLineSymbolStyle, color: Color) =
        Graphic(
            geometry = geometry,
            symbol = SimpleLineSymbol(style, color, 5.0f)
        )

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

    internal fun removeStartingPoint(startingPoint: StartingPoint) {
        _startingPoints.remove(startingPoint)
    }

    private fun addStartingPoint(feature: ArcGISFeature) = try {
        // TODO: add fraction-along to the element.
        // https://devtopia.esri.com/runtime/kotlin/issues/4491
        val utilityElement = utilityNetwork.createElementOrNull(feature)
            ?: throw IllegalArgumentException("could not create utility element from ArcGISFeature")

        val symbol = (feature.featureTable?.layer as FeatureLayer)
                .renderer
                ?.getSymbol(feature)
            ?: throw IllegalArgumentException("could not create drawable from feature symbol")

        _startingPoints.add(
            StartingPoint(
                feature = feature,
                utilityElement = utilityElement,
                symbol = symbol
            )
        )

    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Unit
    }

    private suspend fun identifyFeatures(mapPoint: Point, screenCoordinate: ScreenCoordinate) {
        val result = mapViewProxy.identifyLayers(
            screenCoordinate = screenCoordinate,
            tolerance = 10.dp
        )
        result.onSuccess { identifyLayerResultList ->
            if (identifyLayerResultList.isNotEmpty()) {
                identifyLayerResultList.forEach { identifyLayerResult ->
                    identifyLayerResult.geoElements.filterIsInstance<ArcGISFeature>().forEach { feature ->
                        addStartingPoint(feature)
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

@Immutable
internal data class StartingPoint(val feature: ArcGISFeature, val utilityElement: UtilityElement, val symbol: Symbol) {
    val name: String = utilityElement.assetType.name

    suspend fun getDrawable(metrics: Float): BitmapDrawable =
        symbol.createSwatch(metrics).getOrThrow()
}
