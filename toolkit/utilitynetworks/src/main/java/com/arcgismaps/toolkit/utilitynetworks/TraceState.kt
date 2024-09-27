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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
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
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTraceFunctionOutput
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import kotlinx.coroutines.CancellationException

/**
 * Represents the state for the Trace.
 *
 * @since 200.6.0
 */
@Stable
public class TraceState(
    private val arcGISMap: ArcGISMap,
    private val graphicsOverlay: GraphicsOverlay,
    private val mapViewProxy: MapViewProxy
) {

    private val _initializationStatus: MutableState<InitializationStatus> = mutableStateOf(InitializationStatus.NotInitialized)
    /**
     * The status of the initialization of the state object.
     *
     * @since 200.6.0
     */
    public val initializationStatus: State<InitializationStatus> = _initializationStatus

    private val _traceConfigurations: MutableState<List<UtilityNamedTraceConfiguration>> = mutableStateOf(emptyList())
    /**
     * The named trace configurations of the Utility Network
     *
     * @since 200.6.0
     */
    internal val traceConfigurations: State<List<UtilityNamedTraceConfiguration>> = _traceConfigurations

    private val _addStartingPointMode: MutableState<AddStartingPointMode> = mutableStateOf(AddStartingPointMode.None)
    /**
     * Governs taps on the map. When the mode is [AddStartingPointMode.Started] taps will identify starting points
     * and pass underlying Features to this object.
     *
     * @since 200.6.0
     * @see AddStartingPointMode]
     */
    public val addStartingPointMode: State<AddStartingPointMode> = _addStartingPointMode

    private var _selectedTraceConfiguration: MutableState<UtilityNamedTraceConfiguration?> = mutableStateOf(null)
    /**
     * The selected trace configuration for the TraceParameters that define the trace.
     *
     * @since 200.6.0
     */
    internal val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?> = _selectedTraceConfiguration

    private var _selectedStartingPoint: MutableState<StartingPoint?> = mutableStateOf(null)
    /**
     * The selected starting point to display in the starting point details screen.
     *
     * @since 200.6.0
     */
    internal val selectedStartingPoint: State<StartingPoint?> = _selectedStartingPoint

    private val _currentTraceStartingPoints: SnapshotStateList<StartingPoint> = mutableStateListOf()
    internal val currentTraceStartingPoints: List<StartingPoint> = _currentTraceStartingPoints

    private var _utilityNetwork: UtilityNetwork? = null
    private val utilityNetwork: UtilityNetwork
        get() = _utilityNetwork ?: throw IllegalStateException("Utility Network cannot be null")

    private var _currentTraceRun: MutableState<TraceRun?> = mutableStateOf (null)
    internal val currentTraceRun: State<TraceRun?>
        get() = _currentTraceRun

    private val currentTraceGraphics : MutableList<Graphic> = mutableListOf()

    private val _currentScreen: MutableState<TraceNavRoute> = mutableStateOf(TraceNavRoute.TraceOptions)
    internal var currentScreen: State<TraceNavRoute> = _currentScreen

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
        _utilityNetwork = arcGISMap.utilityNetworks.first()
        _traceConfigurations.value = utilityNetwork.queryNamedTraceConfigurations().getOrThrow()
        _initializationStatus.value = InitializationStatus.Initialized
        return result
    }

    internal fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
    }

    internal fun setSelectedStartingPoint(startingPoint: StartingPoint?) {
        _selectedStartingPoint.value = startingPoint
    }

    internal fun showScreen(screen: TraceNavRoute) {
        _currentScreen.value = screen
    }

    /**
     * Returns the location on the polyline nearest to the tap location.
     * The location is returned as a fraction along the polyline.
     *
     * @param inputGeometry the polyline geometry
     * @param tapPoint the point tapped on the map
     * @return the fraction along the polyline
     * @since 200.6.0
     */
    private fun fractionAlongEdge(inputGeometry: Geometry, tapPoint: Point): Double {
        var polyline = if (inputGeometry is Polyline) inputGeometry else return 0.0

        // Remove Z values from the polyline
        if (polyline.hasZ) {
            polyline = GeometryEngine.createWithZ(polyline, null)
        }
        // confirm spatial reference match
        tapPoint.spatialReference?.let { spatialReference ->
            if (spatialReference != polyline.spatialReference) {
                val projectedGeometry = GeometryEngine.projectOrNull(polyline, spatialReference)
                projectedGeometry?.let { polyline = projectedGeometry }
            }
        }
        return GeometryEngine.fractionAlong(polyline, tapPoint, 10.0)
    }

    /**
     * Determines the point at the given distance along the line. The distance is a
     * fraction of the total length of the line.
     *
     * @param startingPoint the starting point to update
     * @param newValue the new fraction along the edge
     * @since 200.6.0
     */
    internal fun setFractionAlongEdge(startingPoint: StartingPoint, newValue: Double) {
        startingPoint.utilityElement.fractionAlongEdge = newValue
        val geometry = startingPoint.feature.geometry
        if (geometry is Polyline) {
            startingPoint.graphic.geometry = GeometryEngine.createPointAlongOrNull(
                polyline = geometry,
                distance = GeometryEngine.length(geometry) * newValue
            )
        }
    }


    /**
     * Run a trace on the Utility Network using the selected trace configuration and starting points.
     *
     * @return true if the trace results are available, false otherwise.
     * @since 200.6.0
     */
    internal suspend fun trace() : Boolean {
        // Run a trace
        val traceConfiguration = selectedTraceConfiguration.value ?: return false

        if (currentTraceStartingPoints.isEmpty() && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.One) {
            // TODO: Handle error
            Log.i("TraceState --", "ERROR: not enough starting points")
            return false
        }

        if (currentTraceStartingPoints.size < 2 && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.Many) {
            // TODO: Handle error
            Log.i("TraceState --", "ERROR: not enough starting points")
            return false
        }

        val utilityTraceParameters = UtilityTraceParameters(traceConfiguration, currentTraceStartingPoints.map { it.utilityElement })

        val traceResults = utilityNetwork.trace(utilityTraceParameters).getOrElse {
            //handle error
            println("ERROR: running trace" + it.message)
            Log.i("TraceState --", "ERROR: running trace " + it.message)
            emptyList<UtilityElementTraceResult>()
            return false
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
        _currentTraceRun.value = TraceRun(
            name = traceConfiguration.name, // need to auto populate this, if not provided by AdvancedOptions
            graphics = currentTraceGraphics,
            featureResults = currentTraceElementResults,
            functionResults = currentTraceFunctionResults
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
        _currentTraceStartingPoints.remove(startingPoint)
        removeStartingPointGraphic(startingPoint.graphic)
    }

    private fun removeStartingPointGraphic(graphic: Graphic) {
        currentTraceGraphics.remove(graphic)
        graphicsOverlay.graphics.remove(graphic)
    }

    /**
     * This private method is called from a suspend function and so swallows any failures except
     * CancellationExceptions.
     */
    private fun processAndAddStartingPoint(feature: ArcGISFeature, mapPoint: Point) = runCatchingCancellable {
        val utilityElement = utilityNetwork.createElementOrNull(feature)
            ?: throw IllegalArgumentException("could not create utility element from ArcGISFeature")

        // Check if the starting point already exists
        if (_currentTraceStartingPoints.any { it.utilityElement.globalId == utilityElement.globalId }) {
            // TODO: Handle error
            throw IllegalArgumentException("starting point already exists")
        }

        val symbol = (feature.featureTable?.layer as FeatureLayer)
            .renderer
            ?.getSymbol(feature)
            ?: throw IllegalArgumentException("could not create drawable from feature symbol")

        if (utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Edge && feature.geometry is Polyline) {
            feature.geometry?.let { geometry ->
                utilityElement.fractionAlongEdge = fractionAlongEdge(geometry, mapPoint)
            }
        } else if (utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Junction &&
            (utilityElement.assetType.terminalConfiguration?.terminals?.size ?: 0) > 1) {
            utilityElement.terminal = utilityElement.assetType.terminalConfiguration?.terminals?.first()
        }

        val graphic = Graphic(
            geometry = mapPoint,
            symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20.0f)
        )
        graphicsOverlay.graphics.add(graphic)
        currentTraceGraphics.add(graphic)

        _currentTraceStartingPoints.add(
            StartingPoint(
                feature = feature,
                utilityElement = utilityElement,
                symbol = symbol,
                graphic = graphic
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
                _addStartingPointMode.value = AddStartingPointMode.Stopped
                showScreen(TraceNavRoute.TraceOptions)
            }
        }
    }

    /**
     * Set the mode of the state object to activate or deactivate the identification of
     * `GeoElements` in [com.arcgismaps.toolkit.geoviewcompose.MapView] onSingleTapConfirmed response
     * to single tap events.
     *
     * @param status the updated mode
     * @since 200.6.0
     */
    internal fun updateAddStartPointMode(status: AddStartingPointMode) {
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

/**
 * Defines a navigation route for the trace tool screens.
 *
 * @since 200.6.0
 */
internal enum class TraceNavRoute {
    TraceOptions,
    AddStartingPoint,
    TraceResults,
    StartingPointDetails
    //TODO: Add FeatureAttributes route
}

@Immutable
internal data class StartingPoint(val feature: ArcGISFeature, val utilityElement: UtilityElement, val symbol: Symbol, val graphic: Graphic) {
    val name: String = utilityElement.assetType.name

    suspend fun getDrawable(screenScale: Float): BitmapDrawable =
        symbol.createSwatch(screenScale).getOrThrow()
}

@Immutable
internal data class TraceRun(
    val name: String, // need to auto populate this, if not provided by AdvancedOptions
    val graphics: List<Graphic>,
    val featureResults: List<UtilityElement>,
    val functionResults: List<UtilityTraceFunctionOutput>,
)

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