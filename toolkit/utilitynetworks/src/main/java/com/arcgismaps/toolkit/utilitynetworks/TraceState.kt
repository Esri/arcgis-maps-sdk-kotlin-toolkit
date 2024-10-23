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
import androidx.compose.runtime.mutableIntStateOf
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
import com.arcgismaps.geometry.Polyline
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
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTerminal
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

    private var _currentError: Throwable? = null
    internal val currentError: Throwable?
        get() = _currentError

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
    internal val selectedTraceConfiguration: State<UtilityNamedTraceConfiguration?> = _selectedTraceConfiguration

    private var _selectedStartingPoint: MutableState<StartingPoint?> = mutableStateOf(null)
    /**
     * The selected starting point to display in the starting point details screen.
     *
     * @since 200.6.0
     */
    internal val selectedStartingPoint: State<StartingPoint?> = _selectedStartingPoint

    private var _selectedAssetGroupName: String = ""
    internal val selectedAssetGroupName: String
        get() = _selectedAssetGroupName

    private val _currentTraceStartingPoints: SnapshotStateList<StartingPoint> = mutableStateListOf()
    internal val currentTraceStartingPoints: List<StartingPoint> = _currentTraceStartingPoints

    private var _utilityNetwork: UtilityNetwork? = null
    private val utilityNetwork: UtilityNetwork
        get() = _utilityNetwork ?: throw IllegalStateException("Utility Network cannot be null")

    private var currentTraceRun: TraceRun? = null

    private val currentTraceGeometryResultsGraphics: MutableList<Graphic> = mutableListOf()

    private val _completedTraces: SnapshotStateList<TraceRun> = mutableStateListOf()
    internal val completedTraces: List<TraceRun> = _completedTraces

    private var _selectedCompletedTraceIndex: MutableState<Int> = mutableIntStateOf(0)
    internal val selectedCompletedTraceIndex: State<Int> = _selectedCompletedTraceIndex

    private var _isTraceInProgress: MutableState<Boolean> = mutableStateOf(false)
    internal val isTraceInProgress: State<Boolean> = _isTraceInProgress

    private var _isIdentifyInProcess: MutableState<Boolean> = mutableStateOf(false)
    internal val isIdentifyInProcess: State<Boolean> = _isIdentifyInProcess

    private var _currentTraceName: MutableState<String> = mutableStateOf("")
    /**
     * The default name of the trace.
     *
     * @since 200.6.0
     */
    internal val currentTraceName: State<String> = _currentTraceName

    private var currentTraceGraphicsColor: Color = Color.green
    internal val currentTraceGraphicsColorAsComposeColor: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(
            currentTraceGraphicsColor.red,
            currentTraceGraphicsColor.green,
            currentTraceGraphicsColor.blue,
            currentTraceGraphicsColor.alpha
        )

    private var _currentTraceZoomToResults: MutableState<Boolean> = mutableStateOf(true)
    internal var currentTraceZoomToResults: State<Boolean> = _currentTraceZoomToResults

    private var navigateToRoute: ((TraceNavRoute) -> Unit)? = null

    internal fun setNavigationCallback(navigateToRoute: (TraceNavRoute) -> Unit) {
        this.navigateToRoute = navigateToRoute
    }

    /**
     * Initializes the state object by loading the map, the Utility Networks contained in the map
     * and its trace configurations.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.6.0
     */
    internal suspend fun initialize(): Result<Unit> = runCatchingCancellable {
        if (_initializationStatus.value is InitializationStatus.Initialized) {
            return Result.success(Unit)
        }
        _initializationStatus.value = InitializationStatus.Initializing
        arcGISMap.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }
        val utilityNetworks = arcGISMap.utilityNetworks
        if (utilityNetworks.isEmpty()) {
            val error = TraceToolException(TraceError.NO_UTILITY_NETWORK_FOUND)
            _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
            throw error
        }

        utilityNetworks.forEach { utilityNetwork ->
            utilityNetwork.load().getOrElse {
                _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
                throw it
            }
        }
        _utilityNetwork = utilityNetworks.first()
        val traceConfigResult = utilityNetwork.queryNamedTraceConfigurations()
        if (traceConfigResult.isFailure || traceConfigResult.getOrNull().isNullOrEmpty()) {
            val error = TraceToolException(TraceError.NO_TRACE_CONFIGURATIONS_FOUND)
            _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
            throw error
        }
        _traceConfigurations.value = traceConfigResult.getOrThrow()

        _initializationStatus.value = InitializationStatus.Initialized
    }

    internal fun setSelectedTraceConfiguration(config: UtilityNamedTraceConfiguration) {
        _selectedTraceConfiguration.value = config
        _currentTraceName.value = "${config.name} ${(_completedTraces.count { it.configuration.name == config.name } + 1)}"
    }

    internal fun setSelectedStartingPoint(startingPoint: StartingPoint?) {
        _selectedStartingPoint.value = startingPoint
    }

    internal fun showScreen(screen: TraceNavRoute) {
        navigateToRoute?.invoke(screen)
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
    private fun fractionAlongEdge(inputGeometry: Polyline, tapPoint: Point): Double {
        // Remove Z values from the polyline
        var polyline = if (inputGeometry.hasZ) {
            GeometryEngine.createWithZ(inputGeometry, null)
        } else {
            inputGeometry
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
     * Sets the terminal for the starting point.
     *
     * @since 200.6.0
     */
    internal fun setTerminal(startingPoint: StartingPoint, terminal: UtilityTerminal) {
        startingPoint.utilityElement.terminal = terminal
    }

    /**
     * Run a trace on the Utility Network using the selected trace configuration and starting points.
     *
     * @return true if the trace results are available, false otherwise.
     * @since 200.6.0
     */
    internal suspend fun trace(): Result<Unit> = runCatchingCancellable {
        _isTraceInProgress.value = true
        // Run a trace
        val traceConfiguration = selectedTraceConfiguration.value
            ?: throw TraceToolException(TraceError.NO_TRACE_CONFIGURATIONS_FOUND)

        if (currentTraceStartingPoints.isEmpty() && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.One) {
            throw TraceToolException(TraceError.NOT_ENOUGH_STARTING_POINTS_ONE)
        }

        if (currentTraceStartingPoints.size < 2 && traceConfiguration.minimumStartingLocations == UtilityMinimumStartingLocations.Many) {
            throw TraceToolException(TraceError.NOT_ENOUGH_STARTING_POINTS_TWO)
        }

        val utilityTraceParameters =
            UtilityTraceParameters(traceConfiguration, currentTraceStartingPoints.map { it.utilityElement })

        val traceResults = utilityNetwork.trace(utilityTraceParameters).getOrElse {
            throw it
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
                        val graphic = createGraphicForSimpleLineSymbol(polygon, SimpleLineSymbolStyle.Solid, currentTraceGraphicsColor)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    result.polyline?.let { polyline ->
                        val graphic = createGraphicForSimpleLineSymbol(polyline, SimpleLineSymbolStyle.Dash, currentTraceGraphicsColor)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    result.multipoint?.let { multipoint ->
                        val graphic = createGraphicForSimpleLineSymbol(multipoint, SimpleLineSymbolStyle.Dot, currentTraceGraphicsColor)
                        graphicsOverlay.graphics.add(graphic)
                        currentTraceGeometryResultsGraphics.add(graphic)
                    }
                    currentTraceGeometryResults = result
                }
            }
        }

        val currentTraceResultGeometriesExtent = currentTraceGeometryResults?.let {
            getResultGeometriesExtent(it)
        }

        currentTraceRun = TraceRun(
            name = _currentTraceName.value,
            configuration = traceConfiguration,
            startingPoints = _currentTraceStartingPoints.toList(),
            geometryResultsGraphics = currentTraceGeometryResultsGraphics.toList(),
            resultsGraphicExtent = currentTraceResultGeometriesExtent,
            resultGraphicColor = currentTraceGraphicsColorAsComposeColor,
            featureResults = currentTraceElementResults,
            functionResults = currentTraceFunctionResults,
            geometryTraceResult = currentTraceGeometryResults
        ).also {
            _completedTraces.add(it)
            updateSelectedTraceIndexAndGraphics(_completedTraces.size - 1)
        }

        resetCurrentTrace()
    }

    private fun resetCurrentTrace() {
        _selectedTraceConfiguration.value = null
        currentTraceGeometryResultsGraphics.clear()
        _currentTraceName.value = ""
        currentTraceGraphicsColor = Color.green
        _currentTraceZoomToResults.value = true
        _isTraceInProgress.value = false
    }

    private fun getResultGeometriesExtent(utilityGeometryTraceResult: UtilityGeometryTraceResult): Envelope? {
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

    private fun getResultGeometriesExtent(utilityGeometryTraceResult: UtilityGeometryTraceResult): Envelope? {
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

    internal suspend fun zoomToStartingPoint(startingPoint: StartingPoint) {
        startingPoint.graphic.geometry?.let {
            mapViewProxy.setViewpointAnimated(
                Viewpoint(it.extent),
                1.0.seconds,
                AnimationCurve.EaseOutCirc
            )
        }
    }

    private fun updateSelectedTraceIndexAndGraphics(newIndex: Int) {
        updateSelectedStateForTraceResultsGraphics(_selectedCompletedTraceIndex.value, false)
        _selectedCompletedTraceIndex.value = newIndex
        updateSelectedStateForTraceResultsGraphics(_selectedCompletedTraceIndex.value, true)
    }

    private fun updateSelectedStateForTraceResultsGraphics(index: Int, isSelected: Boolean) {
        _completedTraces[index].geometryResultsGraphics.forEach { it.isSelected = isSelected }
        _completedTraces[index].startingPoints.forEach { it.graphic.isSelected = isSelected }
    }

    /**
     * This private method is called from a suspend function and so swallows any failures except
     * CancellationExceptions.
     */
    private fun processAndAddStartingPoint(feature: ArcGISFeature, mapPoint: Point): Result<Unit> = runCatchingCancellable {
        val utilityElement = utilityNetwork.createElementOrNull(feature)
            ?: return@runCatchingCancellable

        // Check if the starting point already exists
        if (_currentTraceStartingPoints.any { it.utilityElement.globalId == utilityElement.globalId }) {
            return@runCatchingCancellable
        }

        val symbol = (feature.featureTable?.layer as FeatureLayer).renderer?.getSymbol(feature)
            ?: throw TraceToolException(TraceError.COULD_NOT_CREATE_DRAWABLE)

        val featureGeometry = feature.geometry
        if (utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Edge && featureGeometry is Polyline) {
            utilityElement.fractionAlongEdge =
                fractionAlongEdge(featureGeometry, mapPoint).takeIf { !it.isNaN() } ?: 0.5
        } else if (utilityElement.networkSource.sourceType == UtilityNetworkSourceType.Junction &&
            (utilityElement.assetType.terminalConfiguration?.terminals?.size ?: 0) > 1
        ) {
            utilityElement.terminal = utilityElement.assetType.terminalConfiguration?.terminals?.first()
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
    }

    private suspend fun identifyFeatures(mapPoint: Point, screenCoordinate: ScreenCoordinate) {
        _isIdentifyInProcess.value = true
        val result = mapViewProxy.identifyLayers(
            screenCoordinate = screenCoordinate,
            tolerance = 10.dp
        )
        result.onSuccess { identifyLayerResultList ->
            val sizeBefore = currentTraceStartingPoints.size
            if (identifyLayerResultList.isNotEmpty()) {
                identifyLayerResultList.forEach { identifyLayerResult ->
                    identifyLayerResult.geoElements.filterIsInstance<ArcGISFeature>().forEach { feature ->
                        processAndAddStartingPoint(feature, mapPoint).getOrElse {
                            setCurrentError(it)
                            _addStartingPointMode.value = AddStartingPointMode.Stopped
                            _isIdentifyInProcess.value = false
                            showScreen(TraceNavRoute.TraceError)
                            return
                        }
                    }
                }
                if (currentTraceStartingPoints.size > sizeBefore) {
                    // If the size of the starting points has changed, then the starting point was added
                    _addStartingPointMode.value = AddStartingPointMode.Stopped
                    _isIdentifyInProcess.value = false
                    showScreen(TraceNavRoute.TraceOptions)
                }
            }
            _isIdentifyInProcess.value = false
        }
        result.onFailure {
            _isIdentifyInProcess.value = false
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

    internal fun setAssetGroupName(name: String) {
        _selectedAssetGroupName = name
    }

    internal fun selectNextCompletedTrace() {
        if (_selectedCompletedTraceIndex.value + 1 < _completedTraces.size) {
            updateSelectedTraceIndexAndGraphics(_selectedCompletedTraceIndex.value + 1)
        }
    }

    internal fun selectPreviousCompletedTrace() {
        if (_selectedCompletedTraceIndex.value - 1 >= 0) {
            updateSelectedTraceIndexAndGraphics(_selectedCompletedTraceIndex.value - 1)
        }
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

    internal fun setGraphicsColorForSelectedTraceRun(color: androidx.compose.ui.graphics.Color) {
        val arcgisColor = Color.fromRgba(
            color.red.toInt() * 255,
            color.green.toInt() * 255,
            color.blue.toInt() * 255,
            color.alpha.toInt() * 255
        )
        val selectedTraceRun = completedTraces[_selectedCompletedTraceIndex.value]
        selectedTraceRun.resultGraphicColor = color

        // update the color of the starting points
        selectedTraceRun.startingPoints.forEach { startingPoint ->
            val symbol = startingPoint.graphic.symbol as SimpleMarkerSymbol
            symbol.color = arcgisColor
        }
        // update the color of the trace results graphics
        selectedTraceRun.geometryResultsGraphics.forEach { graphic ->
            if (graphic.symbol is SimpleLineSymbol) {
                val symbol = graphic.symbol as SimpleLineSymbol
                symbol.color = arcgisColor
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

    internal suspend fun zoomToUtilityElement(utilityElement: UtilityElement) = runCatchingCancellable {
        val features = utilityNetwork.getFeaturesForElements(listOf(utilityElement)).getOrThrow()
        val geometry = features[0].geometry ?: return@runCatchingCancellable
        mapViewProxy.setViewpointAnimated(
            Viewpoint(geometry.extent),
            1.0.seconds,
            AnimationCurve.EaseInOutCubic
        )
    }

    internal fun getAllElementsWithSelectedAssetGroupName(): List<UtilityElement> {
        return completedTraces[_selectedCompletedTraceIndex.value].featureResults.filter { it.assetGroup.name == selectedAssetGroupName }
    }

    internal fun clearAllResults() {
        _completedTraces.clear()
        _selectedCompletedTraceIndex.value = 0
        currentTraceGeometryResultsGraphics.clear()
        _currentTraceStartingPoints.clear()
        graphicsOverlay.graphics.clear()
    }

    internal fun clearSelectedTraceResult() {
        val selectedTrace = _completedTraces[_selectedCompletedTraceIndex.value]
        selectedTrace.geometryResultsGraphics.forEach { graphicsOverlay.graphics.remove(it) }
        selectedTrace.startingPoints.forEach { it.graphic.isSelected = false }
        _completedTraces.removeAt(_selectedCompletedTraceIndex.value)
        if (_selectedCompletedTraceIndex.value - 1 >= 0) {
            _selectedCompletedTraceIndex.value -= 1
            updateSelectedStateForTraceResultsGraphics(_selectedCompletedTraceIndex.value, true)
        }
    }

    internal suspend fun zoomToSelectedTrace() {
        val currentTrace = completedTraces[_selectedCompletedTraceIndex.value]
        val extent = currentTrace.resultsGraphicExtent ?: return
        mapViewProxy.setViewpointAnimated(
            Viewpoint(extent.extent),
            1.0.seconds,
            AnimationCurve.EaseInOutCubic
        )
    }

    /**
     * Set the [error] that occurred during the trace.
     *
     * @since 200.6.0
     */
    internal fun setCurrentError(error: Throwable) {
        _currentError = error
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
    FeatureResultsDetails,
    StartingPointDetails,
    TraceError,
    ClearResults
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
    val startingPoints: List<StartingPoint>,
    val geometryResultsGraphics: List<Graphic>,
    val resultsGraphicExtent: Envelope? = null,
    var resultGraphicColor: androidx.compose.ui.graphics.Color,
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
