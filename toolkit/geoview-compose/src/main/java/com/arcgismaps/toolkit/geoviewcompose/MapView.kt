/*
 *  Copyright 2023 Esri
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
 *
 */

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.BackgroundGrid
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.Grid
import com.arcgismaps.mapping.view.ImageOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.mapping.view.WrapAroundMode
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A compose equivalent of the view-based [MapView].
 *
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable MapView
 * @param modifier Modifier to be applied to the composable MapView
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale]
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry]
 * @param onVisibleAreaChanged lambda invoked when the visible area of the composable MapView has changed
 * @param viewpointPersistence the [ViewpointPersistence] to specify how the viewpoint of the composable MapView is persisted
 * across configuration changes.
 * @param graphicsOverlays graphics overlays used by this composable MapView
 * @param imageOverlays image overlays for displaying images in the composable SceneView
 * @param locationDisplay the [LocationDisplay] used by the composable MapView
 * @param geometryEditor the [GeometryEditor] used by the composable MapView to create and edit geometries by user interaction.
 * @param mapViewProxy the [MapViewProxy] to associate with the composable MapView
 * @param mapViewInteractionOptions the [MapViewInteractionOptions] used by this composable MapView
 * @param viewLabelProperties the [ViewLabelProperties] used by the composable MapView
 * @param selectionProperties the [SelectionProperties] used by the composable MapView
 * @param insets the inset values to control the active visible area, instructing the MapView to ignore parts that may be obstructed
 * by overlaid UI elements and affecting the MapView's logical center, the reported visible area and the location display
 * @param grid represents the display of a coordinate system [Grid] on the composable MapView
 * @param backgroundGrid the default color and context grid behind the map surface
 * @param wrapAroundMode the [WrapAroundMode] to specify whether continuous panning across the international date line is enabled
 * @param isAttributionBarVisible true if attribution bar is visible in the composable MapView, false otherwise
 * @param onAttributionTextChanged lambda invoked when the attribution text of the composable MapView has changed
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes
 * @param timeExtent the [TimeExtent] used by the composable MapView
 * @param onTimeExtentChanged lambda invoked when the composable MapView's [TimeExtent] is changed
 * @param onNavigationChanged lambda invoked when the navigation status of the composable MapView has changed
 * @param onMapRotationChanged lambda invoked when the rotation of this composable MapView has changed
 * @param onMapScaleChanged lambda invoked when the scale of this composable MapView has changed
 * @param onUnitsPerDipChanged lambda invoked when the Units per DIP of this composable MapView has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the composable MapView has changed
 * @param onLayerViewStateChanged lambda invoked when the composable MapView's layer view state is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the composable MapView
 * @param onRotate lambda invoked when a user performs a rotation gesture on the composable MapView
 * @param onScale lambda invoked when a user performs a pinch gesture on the composable MapView
 * @param onUp lambda invoked when the user removes all their pointers from the composable MapView
 * @param onDown lambda invoked when the user first presses on the composable MapView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable MapView
 * @param onDoubleTap lambda invoked the user double taps on the composable MapView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable MapView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable MapView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable MapView
 * @param onDrawStatusChanged lambda invoked when the draw status of the composable MapView is changed
 * @param content the content of the composable MapView
 * @sample com.arcgismaps.toolkit.geoviewcompose.samples.MapViewSample
 * @see
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/display-a-map/">Display a map tutorial</a>
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/display-a-web-map/">Display a web map tutorial</a>
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/add-a-point-line-and-polygon/">Add a point, line, and polygon tutorial</a>
 * @since 200.7.0
 */
@Composable
public fun MapView(
    arcGISMap: ArcGISMap,
    modifier: Modifier = Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    onVisibleAreaChanged: ((Polygon) -> Unit)? = null,
    viewpointPersistence: ViewpointPersistence = MapViewDefaults.DefaultViewpointPersistence,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    imageOverlays: List<ImageOverlay> = remember { emptyList() },
    locationDisplay: LocationDisplay = rememberLocationDisplay(),
    geometryEditor: GeometryEditor? = null,
    mapViewProxy: MapViewProxy? = null,
    mapViewInteractionOptions: MapViewInteractionOptions = remember { MapViewInteractionOptions() },
    viewLabelProperties: ViewLabelProperties = remember { ViewLabelProperties() },
    selectionProperties: SelectionProperties = remember { SelectionProperties() },
    insets: PaddingValues = MapViewDefaults.DefaultInsets,
    grid: Grid? = null,
    backgroundGrid: BackgroundGrid = remember { BackgroundGrid() },
    wrapAroundMode: WrapAroundMode = WrapAroundMode.EnabledWhenSupported,
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)? = null,
    onMapRotationChanged: ((Double) -> Unit)? = null,
    onMapScaleChanged: ((Double) -> Unit)? = null,
    onUnitsPerDipChanged: ((Double) -> Unit)? = null,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)? = null,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)? = null,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)? = null,
    onRotate: ((RotationChangeEvent) -> Unit)? = null,
    onScale: ((ScaleChangeEvent) -> Unit)? = null,
    onUp: ((UpEvent) -> Unit)? = null,
    onDown: ((DownEvent) -> Unit)? = null,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)? = null,
    onDoubleTap: ((DoubleTapEvent) -> Unit)? = null,
    onLongPress: ((LongPressEvent) -> Unit)? = null,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)? = null,
    onPan: ((PanChangeEvent) -> Unit)? = null,
    onDrawStatusChanged: ((DrawStatus) -> Unit)? = null,
    content: (@Composable MapViewScope.() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val layoutDirection = LocalLayoutDirection.current

    // The MapView is wrapped in a Box to ensure that the Callout is drawn on top of the MapView and
    // that the Callout is clipped to its bounds
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "MapView" },
            factory = { mapView },
            update = {
                it.map = arcGISMap
                it.selectionProperties = selectionProperties
                it.interactionOptions = mapViewInteractionOptions
                it.locationDisplay = locationDisplay
                it.labeling = viewLabelProperties
                it.wrapAroundMode = wrapAroundMode
                it.geometryEditor = geometryEditor
                it.grid = grid
                it.backgroundGrid = backgroundGrid
                it.isAttributionBarVisible = isAttributionBarVisible
                it.setTimeExtent(timeExtent)
                if (it.graphicsOverlays != graphicsOverlays) {
                    it.graphicsOverlays.apply {
                        clear()
                        addAll(graphicsOverlays)
                    }
                }
                if (it.imageOverlays != imageOverlays) {
                    it.imageOverlays.apply {
                        clear()
                        addAll(imageOverlays)
                    }
                }
            })

        val mapViewScope = remember { MapViewScope(mapView) }
        val isMapViewReady = mapView.rememberIsReady()

        if (isMapViewReady.value) {
            content?.let {
                mapViewScope.it()
            }
        }
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
            mapView.onDestroy(lifecycleOwner)
        }
    }

    DisposableEffect(mapViewProxy) {
        mapViewProxy?.setMapView(mapView)
        onDispose {
            mapViewProxy?.setMapView(null)
        }
    }

    LaunchedEffect(insets) {
        // When this call is made in the AndroidView's update callback, ViewInsets are not applied
        // on the mapview on initial load. So we set the ViewInsets here.
        mapView.setViewInsets(
            insets.calculateLeftPadding(layoutDirection).value.toDouble(),
            insets.calculateRightPadding(layoutDirection).value.toDouble(),
            insets.calculateTopPadding().value.toDouble(),
            insets.calculateBottomPadding().value.toDouble()
        )
    }

    MapViewEventHandler(
        mapView,
        onTimeExtentChanged,
        onNavigationChanged,
        onMapRotationChanged,
        onMapScaleChanged,
        onUnitsPerDipChanged,
        onSpatialReferenceChanged,
        onLayerViewStateChanged,
        onInteractingChanged,
        onRotate,
        onScale,
        onUp,
        onDown,
        onSingleTapConfirmed,
        onDoubleTap,
        onLongPress,
        onTwoPointerTap,
        onPan,
        onDrawStatusChanged,
        onAttributionTextChanged,
        onAttributionBarLayoutChanged
    )

    ViewpointHandler(
        mapView = mapView,
        viewpointPersistence = viewpointPersistence,
        onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
        onVisibleAreaChanged = onVisibleAreaChanged
    )
}
/**
 * A compose equivalent of the view-based [MapView].
 *
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable MapView
 * @param modifier Modifier to be applied to the composable MapView
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale]
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry]
 * @param onVisibleAreaChanged lambda invoked when the visible area of the composable MapView has changed
 * @param viewpointPersistence the [ViewpointPersistence] to specify how the viewpoint of the composable MapView is persisted
 * across configuration changes.
 * @param graphicsOverlays graphics overlays used by this composable MapView
 * @param locationDisplay the [LocationDisplay] used by the composable MapView
 * @param geometryEditor the [GeometryEditor] used by the composable MapView to create and edit geometries by user interaction.
 * @param mapViewProxy the [MapViewProxy] to associate with the composable MapView
 * @param mapViewInteractionOptions the [MapViewInteractionOptions] used by this composable MapView
 * @param viewLabelProperties the [ViewLabelProperties] used by the composable MapView
 * @param selectionProperties the [SelectionProperties] used by the composable MapView
 * @param insets the inset values to control the active visible area, instructing the MapView to ignore parts that may be obstructed
 * by overlaid UI elements and affecting the MapView's logical center, the reported visible area and the location display
 * @param grid represents the display of a coordinate system [Grid] on the composable MapView
 * @param backgroundGrid the default color and context grid behind the map surface
 * @param wrapAroundMode the [WrapAroundMode] to specify whether continuous panning across the international date line is enabled
 * @param isAttributionBarVisible true if attribution bar is visible in the composable MapView, false otherwise
 * @param onAttributionTextChanged lambda invoked when the attribution text of the composable MapView has changed
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes
 * @param timeExtent the [TimeExtent] used by the composable MapView
 * @param onTimeExtentChanged lambda invoked when the composable MapView's [TimeExtent] is changed
 * @param onNavigationChanged lambda invoked when the navigation status of the composable MapView has changed
 * @param onMapRotationChanged lambda invoked when the rotation of this composable MapView has changed
 * @param onMapScaleChanged lambda invoked when the scale of this composable MapView has changed
 * @param onUnitsPerDipChanged lambda invoked when the Units per DIP of this composable MapView has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the composable MapView has changed
 * @param onLayerViewStateChanged lambda invoked when the composable MapView's layer view state is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the composable MapView
 * @param onRotate lambda invoked when a user performs a rotation gesture on the composable MapView
 * @param onScale lambda invoked when a user performs a pinch gesture on the composable MapView
 * @param onUp lambda invoked when the user removes all their pointers from the composable MapView
 * @param onDown lambda invoked when the user first presses on the composable MapView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable MapView
 * @param onDoubleTap lambda invoked the user double taps on the composable MapView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable MapView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable MapView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable MapView
 * @param onDrawStatusChanged lambda invoked when the draw status of the composable MapView is changed
 * @param content the content of the composable MapView
 * @sample com.arcgismaps.toolkit.geoviewcompose.samples.MapViewSample
 * @see
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/display-a-map/">Display a map tutorial</a>
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/display-a-web-map/">Display a web map tutorial</a>
 * - <a href="https://developers.arcgis.com/kotlin/maps-2d/tutorials/add-a-point-line-and-polygon/">Add a point, line, and polygon tutorial</a>
 * @since 200.4.0
 */
@Deprecated("Deprecation added for backwards compatibility. Use MapView function with imageOverlays instead.", level = DeprecationLevel.HIDDEN)
@Composable
public fun MapView(
    arcGISMap: ArcGISMap,
    modifier: Modifier = Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    onVisibleAreaChanged: ((Polygon) -> Unit)? = null,
    viewpointPersistence: ViewpointPersistence = MapViewDefaults.DefaultViewpointPersistence,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    locationDisplay: LocationDisplay = rememberLocationDisplay(),
    geometryEditor: GeometryEditor? = null,
    mapViewProxy: MapViewProxy? = null,
    mapViewInteractionOptions: MapViewInteractionOptions = remember { MapViewInteractionOptions() },
    viewLabelProperties: ViewLabelProperties = remember { ViewLabelProperties() },
    selectionProperties: SelectionProperties = remember { SelectionProperties() },
    insets: PaddingValues = MapViewDefaults.DefaultInsets,
    grid: Grid? = null,
    backgroundGrid: BackgroundGrid = remember { BackgroundGrid() },
    wrapAroundMode: WrapAroundMode = WrapAroundMode.EnabledWhenSupported,
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)? = null,
    onMapRotationChanged: ((Double) -> Unit)? = null,
    onMapScaleChanged: ((Double) -> Unit)? = null,
    onUnitsPerDipChanged: ((Double) -> Unit)? = null,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)? = null,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)? = null,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)? = null,
    onRotate: ((RotationChangeEvent) -> Unit)? = null,
    onScale: ((ScaleChangeEvent) -> Unit)? = null,
    onUp: ((UpEvent) -> Unit)? = null,
    onDown: ((DownEvent) -> Unit)? = null,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)? = null,
    onDoubleTap: ((DoubleTapEvent) -> Unit)? = null,
    onLongPress: ((LongPressEvent) -> Unit)? = null,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)? = null,
    onPan: ((PanChangeEvent) -> Unit)? = null,
    onDrawStatusChanged: ((DrawStatus) -> Unit)? = null,
    content: (@Composable MapViewScope.() -> Unit)? = null
) {
    // Just call through to the new MapView function that takes imageOverlays
    MapView(
        arcGISMap = arcGISMap,
        modifier = modifier,
        onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
        onVisibleAreaChanged = onVisibleAreaChanged,
        viewpointPersistence = viewpointPersistence,
        graphicsOverlays = graphicsOverlays,
        imageOverlays = remember { emptyList() }, // pass an empty list for imageOverlays
        locationDisplay = locationDisplay,
        geometryEditor = geometryEditor,
        mapViewProxy = mapViewProxy,
        mapViewInteractionOptions = mapViewInteractionOptions,
        viewLabelProperties = viewLabelProperties,
        selectionProperties = selectionProperties,
        insets = insets,
        grid = grid,
        backgroundGrid = backgroundGrid,
        wrapAroundMode = wrapAroundMode,
        isAttributionBarVisible = isAttributionBarVisible,
        onAttributionTextChanged = onAttributionTextChanged,
        onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
        timeExtent = timeExtent,
        onTimeExtentChanged = onTimeExtentChanged,
        onNavigationChanged = onNavigationChanged,
        onMapRotationChanged = onMapRotationChanged,
        onMapScaleChanged = onMapScaleChanged,
        onUnitsPerDipChanged = onUnitsPerDipChanged,
        onSpatialReferenceChanged = onSpatialReferenceChanged,
        onLayerViewStateChanged = onLayerViewStateChanged,
        onInteractingChanged = onInteractingChanged,
        onRotate = onRotate,
        onScale = onScale,
        onUp = onUp,
        onDown = onDown,
        onSingleTapConfirmed = onSingleTapConfirmed,
        onDoubleTap = onDoubleTap,
        onLongPress = onLongPress,
        onTwoPointerTap = onTwoPointerTap,
        onPan = onPan,
        onDrawStatusChanged = onDrawStatusChanged,
        content = content
    )
}

/**
 * Sets up the callbacks for all the view-based [mapView] events except the viewpoint changed events.
 */
@Composable
private fun MapViewEventHandler(
    mapView: MapView,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)?,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)?,
    onMapRotationChanged: ((Double) -> Unit)?,
    onMapScaleChanged: ((Double) -> Unit)?,
    onUnitsPerDipChanged: ((Double) -> Unit)?,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)?,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)?,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)?,
    onRotate: ((RotationChangeEvent) -> Unit)?,
    onScale: ((ScaleChangeEvent) -> Unit)?,
    onUp: ((UpEvent) -> Unit)?,
    onDown: ((DownEvent) -> Unit)?,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)?,
    onDoubleTap: ((DoubleTapEvent) -> Unit)?,
    onLongPress: ((LongPressEvent) -> Unit)?,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)?,
    onPan: ((PanChangeEvent) -> Unit)?,
    onDrawStatusChanged: ((DrawStatus) -> Unit)?,
    onAttributionTextChanged: ((String) -> Unit)?,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)?
) {
    val currentTimeExtentChanged by rememberUpdatedState(onTimeExtentChanged)
    val currentOnNavigationChanged by rememberUpdatedState(onNavigationChanged)
    val currentOnMapRotationChanged by rememberUpdatedState(onMapRotationChanged)
    val currentOnMapScaleChanged by rememberUpdatedState(onMapScaleChanged)
    val currentOnUnitsPerDipChanged by rememberUpdatedState(onUnitsPerDipChanged)
    val currentOnSpatialReferenceChanged by rememberUpdatedState(onSpatialReferenceChanged)
    val currentOnInteractingChanged by rememberUpdatedState(onInteractingChanged)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnUp by rememberUpdatedState(onUp)
    val currentOnDown by rememberUpdatedState(onDown)
    val currentSingleTapConfirmed by rememberUpdatedState(onSingleTapConfirmed)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTwoPointerTap by rememberUpdatedState(onTwoPointerTap)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentOnDrawStatusChanged by rememberUpdatedState(onDrawStatusChanged)
    val currentOnLayerViewStateChanged by rememberUpdatedState(onLayerViewStateChanged)
    val currentOnAttributionTextChanged by rememberUpdatedState(onAttributionTextChanged)
    val currentOnAttributionBarLayoutChanged by rememberUpdatedState(onAttributionBarLayoutChanged)

    LaunchedEffect(Unit) {
        launch {
            mapView.timeExtent.collect { currentTimeExtent ->
                currentTimeExtentChanged?.invoke(currentTimeExtent)
            }
        }
        launch {
            mapView.layerViewStateChanged.collect { currentLayerViewState ->
                currentOnLayerViewStateChanged?.invoke(currentLayerViewState)
            }
        }
        launch {
            mapView.mapRotation.collect { mapRotation ->
                currentOnMapRotationChanged?.invoke(mapRotation)
            }
        }
        launch {
            mapView.mapScale.collect { mapScale ->
                currentOnMapScaleChanged?.invoke(mapScale)
                currentOnUnitsPerDipChanged?.invoke(mapView.unitsPerDip)
            }
        }
        launch {
            mapView.spatialReference.collect { spatialReference ->
                currentOnSpatialReferenceChanged?.invoke(spatialReference)
            }
        }
        launch {
            mapView.navigationChanged.collect {
                currentOnNavigationChanged?.invoke(it)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.isInteracting.collect { isInteracting ->
                currentOnInteractingChanged?.invoke(isInteracting)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onRotate.collect { rotationChangeEvent ->
                currentOnRotate?.invoke(rotationChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onScale.collect { scaleChangeEvent ->
                currentOnScale?.invoke(scaleChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onUp.collect { upEvent ->
                currentOnUp?.invoke(upEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onDown.collect { downEvent ->
                currentOnDown?.invoke(downEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onSingleTapConfirmed.collect { singleTapConfirmedEvent ->
                currentSingleTapConfirmed?.invoke(singleTapConfirmedEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onDoubleTap.collect { doubleTapEvent ->
                currentOnDoubleTap?.invoke(doubleTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onLongPress.collect { longPressEvent ->
                currentOnLongPress?.invoke(longPressEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onTwoPointerTap.collect { twoPointerTapEvent ->
                currentOnTwoPointerTap?.invoke(twoPointerTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onPan.collect { panChangeEvent ->
                currentOnPan?.invoke(panChangeEvent)
            }
        }
        launch {
            mapView.drawStatus.collect { drawStatus ->
                currentOnDrawStatusChanged?.invoke(drawStatus)
            }
        }
        launch {
            mapView.attributionText.collect { attributionText ->
                currentOnAttributionTextChanged?.invoke(attributionText)
            }
        }
        launch {
            mapView.onAttributionBarLayoutChanged.collect { attributionBarLayoutChangeEvent ->
                currentOnAttributionBarLayoutChanged?.invoke(attributionBarLayoutChangeEvent)
            }
        }
    }
}

/**
 * Handles viewpoint change events and persistence for a [MapView].
 *
 * @since 200.4.0
 */
@Composable
private fun ViewpointHandler(
    mapView: MapView,
    viewpointPersistence: ViewpointPersistence,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)?,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)?,
    onVisibleAreaChanged: ((Polygon) -> Unit)?
) {
    val currentOnViewpointChangedForCenterAndScale by rememberUpdatedState(
        onViewpointChangedForCenterAndScale
    )
    val currentOnViewpointChangedForBoundingGeometry by rememberUpdatedState(
        onViewpointChangedForBoundingGeometry
    )
    val currentVisibleAreaChanged by rememberUpdatedState(onVisibleAreaChanged)
    val currentViewpointPersistence by rememberUpdatedState(viewpointPersistence)
    var persistedViewpoint by rememberSaveable(
        saver = Saver(
            save = {
                it.value?.toJson()
            },
            restore = {
                mutableStateOf(Viewpoint.fromJsonOrNull(it))
            }
        )
    ) {
        mutableStateOf<Viewpoint?>(null)
    }

    LaunchedEffect(Unit) {
        // if there is a persisted viewpoint, restore it when the SceneView enters the composition
        persistedViewpoint?.let { mapView.setViewpoint(it) }
        launch {
            mapView.viewpointChanged.collect {
                val newViewpoint = mapView.getViewpointByPersistence(currentViewpointPersistence)
                newViewpoint?.let {
                    persistedViewpoint = it
                }

                currentOnViewpointChangedForCenterAndScale?.let { callback ->
                    val currentViewpoint =
                        if (newViewpoint?.viewpointType != ViewpointType.CenterAndScale) {
                            mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                        } else newViewpoint
                    currentViewpoint?.let(callback)
                }

                currentOnViewpointChangedForBoundingGeometry?.let { callback ->
                    val currentViewpoint =
                        if (newViewpoint?.viewpointType != ViewpointType.BoundingGeometry) {
                            mapView.getCurrentViewpoint(ViewpointType.BoundingGeometry)
                        } else newViewpoint
                    currentViewpoint?.let(callback)
                }

                currentVisibleAreaChanged?.invoke(
                    mapView.visibleArea
                        ?: throw IllegalStateException("MapView visible area should not be null")
                )
            }
        }
        launch {
            // For performance reasons we use snapshotFlow instead of LaunchedEffect(viewpointPersistence)
            // here in order to keep track of changes to currentViewpointPersistence at recomposition
            snapshotFlow { currentViewpointPersistence }
                .collect {
                    persistedViewpoint = mapView.getViewpointByPersistence(it)
                }
        }
    }
}

/**
 * Returns the current viewpoint of the [MapView] with the appropriate [ViewpointType] based on [viewpointPersistence].
 *
 * @since 200.4.0
 */
private fun MapView.getViewpointByPersistence(viewpointPersistence: ViewpointPersistence): Viewpoint? =
    when (viewpointPersistence) {
        is ViewpointPersistence.None -> null
        is ViewpointPersistence.ByCenterAndScale -> getCurrentViewpoint(ViewpointType.CenterAndScale)
        is ViewpointPersistence.ByBoundingGeometry -> getCurrentViewpoint(ViewpointType.BoundingGeometry)
    }

/**
 * Create and [remember] a [LocationDisplay].
 * Checks that [ArcGISEnvironment.applicationContext] is set and if not, sets one.
 * [init] will be called when the [LocationDisplay] is first created to configure its
 * initial state.
 *
 * @param key invalidates the remembered LocationDisplay if different from the previous composition
 * @param init called when the [LocationDisplay] is created to configure its initial state
 * @since 200.4.0
 */
@Composable
public inline fun rememberLocationDisplay(
    key: Any? = null,
    crossinline init: LocationDisplay.() -> Unit = {}
): LocationDisplay {
    if (ArcGISEnvironment.applicationContext == null) {
        ArcGISEnvironment.applicationContext = LocalContext.current
    }
    return remember(key) {
        LocationDisplay().apply(init)
    }
}

/**
 * The receiver class of the [MapView] content lambda.
 *
 * @since 200.5.0
 */
public class MapViewScope internal constructor(mapView: MapView) : GeoViewScope(mapView)

/**
 * Contains default values for the composable MapView.
 *
 * @see com.arcgismaps.toolkit.geoviewcompose.MapView
 * @since 200.4.0
 */
public object MapViewDefaults {

    /**
     * Default insets for the composable MapView, set to 0 on all sides.
     *
     * @since 200.4.0
     */
    public val DefaultInsets: PaddingValues = PaddingValues()

    /**
     * Default viewpoint persistence for the composable MapView, set to [ViewpointPersistence.ByCenterAndScale].
     *
     * @since 200.4.0
     */
    public val DefaultViewpointPersistence: ViewpointPersistence =
        ViewpointPersistence.ByCenterAndScale()
}
