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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.AnimationCurve
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
import kotlin.time.Duration.Companion.seconds

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

    private val _currentError: MutableState<Throwable?> = mutableStateOf(null)
    internal val currentError: State<Throwable?> = _currentError
    private val _initializationStatus: MutableState<InitializationStatus> =
        mutableStateOf(InitializationStatus.NotInitialized)

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
    public val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?> = _selectedTraceConfiguration

    private val _currentTraceStartingPoints: SnapshotStateList<StartingPoint> = mutableStateListOf()
    internal val currentTraceStartingPoints: List<StartingPoint> = _currentTraceStartingPoints

    private var _utilityNetwork: UtilityNetwork? = null
    private val utilityNetwork: UtilityNetwork
        get() = _utilityNetwork ?: throw IllegalStateException("Utility Network cannot be null")

    private var _currentTraceRun: MutableState<TraceRun?> = mutableStateOf(null)
    internal val currentTraceRun: State<TraceRun?>
        get() = _currentTraceRun

    private val currentTraceGeometryResultsGraphics: MutableList<Graphic> = mutableListOf()

    private val _currentScreen: MutableState<TraceNavRoute> = mutableStateOf(TraceNavRoute.TraceOptions)
    internal var currentScreen: State<TraceNavRoute> = _currentScreen

    private val completedTraces: MutableList<TraceRun> = mutableListOf()

    private var _currentTraceName: MutableState<String> = mutableStateOf("")

    /**
     * The default name of the trace.
     *
     * @since 200.6.0
     */
    public val currentTraceName: State<String> = _currentTraceName

    private var currentTraceGraphicsColor: Color = Color.green
    public val currentTraceGraphicsColorAsComposeColor: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(
            currentTraceGraphicsColor.red,
            currentTraceGraphicsColor.green,
            currentTraceGraphicsColor.blue,
            currentTraceGraphicsColor.alpha
        )

    private var _currentTraceZoomToResults: MutableState<Boolean> = mutableStateOf(false)
    public var currentTraceZoomToResults: State<Boolean> = _currentTraceZoomToResults

    private val currentTraceResultGeometriesExtent: Envelope?
        get() {
            val utilityGeometryTraceResult = _currentTraceRun.value?.geometryTraceResult ?: return null

            val geometries = listOf(
                utilityGeometryTraceResult.polygon,
                utilityGeometryTraceResult.polyline,
                utilityGeometryTraceResult.multipoint
            ).mapNotNull { geometry ->
                if (geometry != null && !geometry.isEmpty) {
                    geometry
                } else {
                    null
                }
            }
            val combinedExtents = GeometryEngine.combineExtentsOrNull(geometries) ?: return null
            val expandedEnvelope = GeometryEngine.bufferOrNull(combinedExtents, 200.0) ?: return null

            return expandedEnvelope.extent
        }

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
        arcGISMap.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            return Result.failure(it)
        }
        val utilityNetworks = arcGISMap.utilityNetworks
        if (utilityNetworks.isEmpty()) {
            val error = IllegalStateException("No Utility Network found.")
            _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
            return Result.failure(error)
        }

        utilityNetworks.forEach { utilityNetwork ->
            utilityNetwork.load().getOrElse {
                _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
                return Result.failure(it)
            }
        }
        _utilityNetwork = utilityNetworks.first()
        val traceConfigResult = utilityNetwork.queryNamedTraceConfigurations()
        if (traceConfigResult.isFailure || traceConfigResult.getOrNull().isNullOrEmpty()) {
            val error = IllegalStateException("No trace configurations found.")
            _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
            return Result.failure(error)
        }
        _traceConfigurations.value = traceConfigResult.getOrThrow()

        _initializationStatus.value = InitializationStatus.Initialized
        return Result.success(Unit)
    }

    internal fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
        _currentTraceName.value =
            "${config.name} ${(completedTraces.count { it.configuration.name == config.name } + 1)}"
    }

    internal fun showScreen(screen: TraceNavRoute) {
        _currentScreen.value = screen
    }

    /**
     * Run a trace on the Utility Network using the selected trace configuration and starting points.
     *
     * @return true if the trace results are available, false otherwise.
     * @since 200.6.0
     */
    internal suspend fun trace(): Boolean {
        // Run a trace
        val traceConfiguration = selectedTraceConfiguration.value ?: return false

        if (currentTraceStartingPoints.isEmpty() && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.One) {
            setCurrentError(IllegalArgumentException("Not enough starting points"))
            return false
        }

        if (currentTraceStartingPoints.size > 2 && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.Many) {
            setCurrentError(IllegalArgumentException("Not enough starting points"))
            return false
        }

        val utilityTraceParameters =
            UtilityTraceParameters(traceConfiguration, currentTraceStartingPoints.map { it.utilityElement })

        val traceResults = utilityNetwork.trace(utilityTraceParameters).getOrElse {
            setCurrentError(it)
            emptyList<UtilityElementTraceResult>()
            return false
        }

        var currentTraceFunctionResults: List<UtilityTraceFunctionOutput> = emptyList()
        var currentTraceElementResults: List<UtilityElement> = emptyList()
        var currentTraceGeometryResults: UtilityGeometryTraceResult? = null

        for (result in traceResults) {
            when (result) {
                // Feature results
                is UtilityElementTraceResult -> {
                    currentTraceElementResults = result.elements
                }
                // Function results
                is UtilityFunctionTraceResult -> {
                    currentTraceFunctionResults = result.functionOutputs
                }
                // Geometry results
                is UtilityGeometryTraceResult -> {
                    result.polygon?.let { polygon ->
                        val graphic = createGraphicForSimpleLineSymbol(
                            polygon,
                            SimpleLineSymbolStyle.Solid,
                            currentTraceGraphicsColor
                        )
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    result.polyline?.let { polyline ->
                        val graphic = createGraphicForSimpleLineSymbol(
                            polyline,
                            SimpleLineSymbolStyle.Dash,
                            currentTraceGraphicsColor
                        )
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    result.multipoint?.let { multipoint ->
                        val graphic = createGraphicForSimpleLineSymbol(
                            multipoint,
                            SimpleLineSymbolStyle.Dot,
                            currentTraceGraphicsColor
                        )
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    currentTraceGeometryResults = result
                    // Highlight the geometry results
                    currentTraceGeometryResultsGraphics.map { it.isSelected = true }
                }
            }
        }
        _currentTraceRun.value = TraceRun(
            name = _currentTraceName.value,
            configuration = traceConfiguration,
            graphics = currentTraceGeometryResultsGraphics,
            featureResults = currentTraceElementResults,
            functionResults = currentTraceFunctionResults,
            geometryTraceResult = currentTraceGeometryResults
        ).also { completedTraces.add(it) }

        if (_currentTraceZoomToResults.value) {
            currentTraceResultGeometriesExtent?.let {
                mapViewProxy.setViewpointAnimated(
                    Viewpoint(it),
                    2.0.seconds,
                    AnimationCurve.EaseOutCirc
                )
            }
        }

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
        graphicsOverlay.graphics.remove(startingPoint.graphic)
    }

    /**
     * This private method is called from a suspend function and so swallows any failures except
     * CancellationExceptions.
     */
    private fun processAndAddStartingPoint(feature: ArcGISFeature, mapPoint: Point): Boolean {
        // TODO: add fraction-along to the element.
        // https://devtopia.esri.com/runtime/kotlin/issues/4491
        val utilityElement = utilityNetwork.createElementOrNull(feature)
        if (utilityElement == null) {
            setCurrentError(IllegalArgumentException("Could not create utility element from ArcGISFeature."))
            return false
        }

        // Check if the starting point already exists
        if (_currentTraceStartingPoints.any { it.utilityElement.globalId == utilityElement.globalId }) {
            val exception = IllegalArgumentException("One or more starting points already exists.")
            setCurrentError(exception)
            return false
        }

        val symbol = (feature.featureTable?.layer as FeatureLayer).renderer?.getSymbol(feature)
        if (symbol == null) {
            setCurrentError(IllegalArgumentException("Could not create drawable from feature symbol"))
            return false
        }

        val graphic = Graphic(
            geometry = mapPoint,
            symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, currentTraceGraphicsColor, 20.0f)
        )
        graphicsOverlay.graphics.add(graphic)

        _currentTraceStartingPoints.add(
            StartingPoint(
                feature = feature,
                utilityElement = utilityElement,
                symbol = symbol,
                graphic = graphic
            )
        )
        return true
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
                        if (!processAndAddStartingPoint(feature, mapPoint)) {
                            _addStartingPointMode.value = AddStartingPointMode.Stopped
                            showScreen(TraceNavRoute.TraceError)
                            return
                        }
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

    /**
     * Set the name of the trace.
     *
     * @param name the name of the trace
     * @since 200.6.0
     */
    internal fun setTraceName(name: String) {
        _currentTraceName.value = name
    }

    /**
     * Set the color of the graphics.
     *
     * @param color the color of the graphics
     * @since 200.6.0
     */
    internal fun setGraphicsColor(color: androidx.compose.ui.graphics.Color) {
        currentTraceGraphicsColor = Color.fromRgba(
            color.red.toInt() * 255,
            color.green.toInt() * 255,
            color.blue.toInt() * 255,
            color.alpha.toInt() * 255
        )
        // update the color of the starting points
        _currentTraceStartingPoints.forEach { startingPoint ->
            val symbol = startingPoint.graphic.symbol as SimpleMarkerSymbol
            symbol.color = currentTraceGraphicsColor
        }
        // update the color of the trace results graphics
        currentTraceGeometryResultsGraphics.forEach { graphic ->
            if (graphic.symbol is SimpleLineSymbol) {
                val symbol = graphic.symbol as SimpleLineSymbol
                symbol.color = currentTraceGraphicsColor
            }
        }
    }

    /**
     * Set whether to zoom to the results.
     *
     * @param zoom whether to zoom to the results
     * @since 200.6.0
     */
    internal fun setZoomToResults(zoom: Boolean) {
        _currentTraceZoomToResults.value = zoom
    }

    /**
     * Set the current error, if any.
     * @since 200.6.0
     */
    internal fun setCurrentError(error: Throwable?) {
        _currentError.value = error
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
    TraceError
    //TODO: Add FeatureAttributes route
}

@Immutable
internal data class StartingPoint(
    val feature: ArcGISFeature,
    val utilityElement: UtilityElement,
    val symbol: Symbol,
    val graphic: Graphic
) {
    val name: String = utilityElement.assetType.name

    suspend fun getDrawable(screenScale: Float): BitmapDrawable =
        symbol.createSwatch(screenScale).getOrThrow()
}

@Immutable
internal data class TraceRun(
    val name: String, // need to auto populate this, if not provided by AdvancedOptions
    val configuration: UtilityNamedTraceConfiguration,
    val graphics: List<Graphic>,
    val featureResults: List<UtilityElement>,
    val functionResults: List<UtilityTraceFunctionOutput>,
    val geometryTraceResult: UtilityGeometryTraceResult?
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
