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
import kotlinx.coroutines.CancellationException
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

    private val _initializationStatus = MutableStateFlow<InitializationStatus>(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.6.0
     */
    public val initializationStatus: StateFlow<InitializationStatus> = _initializationStatus

    private val _traceConfigurations = MutableStateFlow<List<UtilityNamedTraceConfiguration>>(emptyList())

    /**
     * The named trace configurations of the Utility Network
     *
     * @since 200.6.0
     */
    internal val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>> = _traceConfigurations.asStateFlow()

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

    /**
     * Initializes the state object by loading the map, the Utility Networks contained in the map
     * and its trace configurations.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.6.0
     */
    internal suspend fun initialize(): Result<Unit> {
        if (_initializationStatus.value is InitializationStatus.Initialized) {
            return Result.success(Unit)
        }
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
        _utilityNetwork = arcGISMap.utilityNetworks.first()
        _traceConfigurations.value = utilityNetwork.queryNamedTraceConfigurations().getOrThrow()
        return result
    }

    internal fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
    }

    private var _currentTraceRun: TraceRun? = null
    internal val currentTraceRun: TraceRun
        get() = _currentTraceRun ?: throw IllegalStateException("TraceRun cannot be null")

    private val currentTraceGraphics : MutableList<Graphic> = mutableListOf()

    /**
     * Run a trace on the Utility Network using the selected trace configuration and starting points.
     *
     * @return true if the trace results are available, false otherwise.
     * @since 200.6.0
     */
    internal suspend fun trace() : Boolean {
        // Run a trace
        val traceConfiguration = selectedTraceConfiguration.value ?: return false

        if (startingPoints.isEmpty() && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.One) {
            // TODO: Handle error
            return false
        }

        if (startingPoints.size < 2 && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.Many) {
            // TODO: Handle error
            return false
        }

        val utilityTraceParameters = UtilityTraceParameters(traceConfiguration, startingPoints.map { it.utilityElement })

        val traceResults = utilityNetwork.trace(utilityTraceParameters).getOrElse {
            //handle error
            println("ERROR: running trace" + it.message)
            emptyList<UtilityElementTraceResult>()
        }

        val currentTraceFunctionResults : MutableList<UtilityTraceFunctionOutput> = mutableListOf()
        var currentTraceElementResults: List<UtilityElement> = emptyList()

        for (result in traceResults) {
            when (result) {
                // Feature results
                is UtilityElementTraceResult -> {
                    currentTraceElementResults = result.elements
                }
                // Function results
                is UtilityFunctionTraceResult -> {
                    result.functionOutputs.forEach {
                        currentTraceFunctionResults.add(it)
                    }
                }
                // Geometry results
                is UtilityGeometryTraceResult -> {
                    result.polygon?.let { polygon ->
                        val graphic = createGraphicForSimpleLineSymbol(polygon, SimpleLineSymbolStyle.Solid, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    result.polyline?.let { polyline ->
                        val graphic = createGraphicForSimpleLineSymbol(polyline, SimpleLineSymbolStyle.Dash, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    result.multipoint?.let { multipoint ->
                        val graphic = createGraphicForSimpleLineSymbol(multipoint, SimpleLineSymbolStyle.Dot, Color.green)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGraphics.add(graphic)
                    }
                    // Highlight the geometry results
                    currentTraceGraphics.map { it.isSelected = true }
                }
            }
        }
        _currentTraceRun = TraceRun(
            name = traceConfiguration.name, // need to auto populate this, if not provided by AdvancedOptions
            graphics = currentTraceGraphics,
            featureResults = currentTraceElementResults,
            functionResults = currentTraceFunctionResults,
            geometryResults = currentTraceGraphics
        )
        return true
    }

    private fun createGraphicForSimpleLineSymbol(geometry: Geometry, style: SimpleLineSymbolStyle, color: Color) =
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

    /**
     * This private method is called from a suspend function and so swallows any failures except
     * CancellationExceptions.
     */
    private fun processAndAddStartingPoint(feature: ArcGISFeature, mapPoint: Point) = runCatchingCancellable {
        // TODO: add fraction-along to the element.
        // https://devtopia.esri.com/runtime/kotlin/issues/4491
        val utilityElement = utilityNetwork.createElementOrNull(feature)
            ?: throw IllegalArgumentException("could not create utility element from ArcGISFeature")

        val symbol = (feature.featureTable?.layer as FeatureLayer)
            .renderer
            ?.getSymbol(feature)
            ?: throw IllegalArgumentException("could not create drawable from feature symbol")

        val graphic = Graphic(
            geometry = mapPoint,
            symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20.0f)
        )
        graphicsOverlay.graphics.add(graphic)
        currentTraceGraphics.add(graphic)

        _startingPoints.add(
            StartingPoint(
                feature = feature,
                utilityElement = utilityElement,
                symbol = symbol
            )
        )
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
                        processAndAddStartingPoint(feature, mapPoint)
                    }
                }
//                addTapLocationToGraphicsOverlay(mapPoint)
                _addStartingPointMode.value = AddStartingPointMode.Stopped
            }
        }
    }

    private fun addTapLocationToGraphicsOverlay(mapPoint: Point) {
        val graphic = Graphic(
            geometry = mapPoint,
            symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20.0f)
        )
        graphicsOverlay.graphics.add(graphic)
        currentTraceGraphics.add(graphic)
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

@Immutable
internal data class StartingPoint(val feature: ArcGISFeature, val utilityElement: UtilityElement, val symbol: Symbol) {
    val name: String = utilityElement.assetType.name

    suspend fun getDrawable(screenScale: Float): BitmapDrawable =
        symbol.createSwatch(screenScale).getOrThrow()
}

/**
 * Returns [this] Result, but if it is a failure with the specified exception type, then it throws the exception.
 *
 * @param T a [Throwable] type which should be thrown instead of encapsulated in the [Result].
 */
internal inline fun <reified T : Throwable, R> Result<R>.except(): Result<R> = onFailure { if (it is T) throw it }

/**
 * Runs the specified [block] with [this] value as its receiver and catches any exceptions, returning a `Result` with the
 * result of the block or the exception. If the exception is a [CancellationException], the exception will not be encapsulated
 * in the failure but will be rethrown.
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> =
    runCatching(block)
        .except<CancellationException, R>()

@Immutable
internal data class TraceRun(
    val name: String, // need to auto populate this, if not provided by AdvancedOptions
    val graphics: List<Graphic>,
    val featureResults: List<UtilityElement>,
    val functionResults: List<UtilityTraceFunctionOutput>,
    val geometryResults: List<Graphic>
)
