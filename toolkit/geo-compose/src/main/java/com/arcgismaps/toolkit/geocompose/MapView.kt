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

package com.arcgismaps.toolkit.geocompose

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.view.BackgroundGrid
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.Grid
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.mapping.view.WrapAroundMode
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@SuppressLint("StaticFieldLeak")
//internal val context = LocalContext.current
//@SuppressLint("StaticFieldLeak")
@SuppressLint("StaticFieldLeak")
internal var mapView: MapView? = null
internal val LocalMapView = compositionLocalOf<MapView?> { mapView }

/**
 * A compose equivalent of the view-based [MapView].
 *
 * @param modifier Modifier to be applied to the composable MapView
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable MapView
 * @param viewpointOperation a [MapViewpointOperation] that changes this MapView to a new viewpoint
 * @param viewpointChangedState specifies lambdas invoked when the viewpoint of the composable MapView has changed
 * @param onVisibleAreaChanged lambda invoked when the visible area of the composable MapView has changed
 * @param graphicsOverlays the [GraphicsOverlayCollection] used by this composable MapView
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
 * @param attributionState specifies the attribution bar's visibility, text changed and layout changed events
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
 * @since 200.4.0
 */
@Composable
public fun MapView(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap? = null,
    viewpointOperation: MapViewpointOperation? = null,
    viewpointChangedState: ViewpointChangedState? = null,
    onVisibleAreaChanged: ((Polygon) -> Unit)? = null,
    graphicsOverlays: GraphicsOverlayCollection = rememberGraphicsOverlayCollection(),
    locationDisplay: LocationDisplay = rememberLocationDisplay(),
    geometryEditor: GeometryEditor? = null,
    mapViewProxy: MapViewProxy? = null,
    mapViewInteractionOptions: MapViewInteractionOptions = MapViewInteractionOptions(),
    calloutPlacementOperation: CalloutPlacementOperation? = null,
    viewLabelProperties: ViewLabelProperties = ViewLabelProperties(),
    selectionProperties: SelectionProperties = SelectionProperties(),
    insets: PaddingValues = PaddingValues(),
    grid: Grid? = null,
    backgroundGrid: BackgroundGrid = BackgroundGrid(),
    wrapAroundMode: WrapAroundMode = WrapAroundMode.EnabledWhenSupported,
    attributionState: AttributionState = AttributionState(),
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
//    content: @Composable ((mapView: MapView) -> Unit)? = null,
//    content: @Composable ((mapViewProxy: MapViewProxy?) -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
//    callout: Callout? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
//    val mapView = remember { MapView(context) }
    mapView = remember { MapView(context) }
    val layoutDirection = LocalLayoutDirection.current
//    val calloutScreenCoordinate: ScreenCoordinate? = callout?.let { mapView.locationToScreen(it.location) }
    var updateCalloutPosition by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalMapView provides mapView) {

        Box(modifier = modifier.semantics {
            contentDescription = "MapContainer"
        }) {
            AndroidView(
                modifier = modifier.semantics { contentDescription = "MapView" },
                factory = { mapView!! },
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
                    it.setTimeExtent(timeExtent)
//                    if (content != null) {
//
//                        content(mapView)
//                    }
                })

            if (content != null) {
                content()
            }

//            ShowCallout(mapView, callout, updateCalloutPosition)
//            Box(
////                modifier = Modifier.offset(x = calloutScreenCoordinate.x.dp, y = calloutScreenCoordinate.y.dp)
////                modifier = Modifier.offset(x = 186.dp, y = 50.dp)
//                modifier = Modifier.offset(
//                    x = with(LocalDensity.current) { calloutScreenCoordinate.x.toFloat().toDp() },
//                    y = with(LocalDensity.current) { calloutScreenCoordinate.y.toFloat().toDp() })
//                    .wrapContentSize()
////                    .padding(30.dp)
//                    .background(Color.White)
//                    .border(
//                        border = BorderStroke(2.dp, Color.LightGray),
//                        shape = MaterialTheme.shapes.medium
//                    )
//            )
//            {
//                callout.content?.invoke()
//            }
        }

        DisposableEffect(Unit) {
            lifecycleOwner.lifecycle.addObserver(mapView!!)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(mapView!!)
                mapView!!.onDestroy(lifecycleOwner)
            }
        }

        ViewpointUpdater(mapView!!, viewpointOperation)

//    CalloutUpdater(mapView, calloutPlacementOperation)

        DisposableEffect(mapViewProxy) {
            mapViewProxy?.setMapView(mapView)
            onDispose {
                mapViewProxy?.setMapView(null)
            }
        }
        LaunchedEffect(Unit) {
            launch {
                mapView!!.viewpointChanged.collect {
                    updateCalloutPosition = !updateCalloutPosition
                }
            }
        }

        LaunchedEffect(insets) {
            // When this call is made in the AndroidView's update callback, ViewInsets are not applied
            // on the mapview on initial load. So we set the ViewInsets here.
            mapView!!.setViewInsets(
                insets.calculateLeftPadding(layoutDirection).value.toDouble(),
                insets.calculateRightPadding(layoutDirection).value.toDouble(),
                insets.calculateTopPadding().value.toDouble(),
                insets.calculateBottomPadding().value.toDouble()
            )
        }

        AttributionStateHandler(mapView!!, attributionState)
        ViewpointChangedStateHandler(mapView!!, viewpointChangedState)

        MapViewEventHandler(
            mapView!!,
            onTimeExtentChanged,
            onVisibleAreaChanged,
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
            onDrawStatusChanged
        )

        GraphicsOverlaysUpdater(graphicsOverlays, mapView!!)
    }
}

/**
 * Updates the viewpoint of the provided view-based [mapView] using the given [viewpointOperation]. This will be
 * recomposed when [viewpointOperation] changes.
 *
 * @since 200.4.0
 */
@Composable
private fun ViewpointUpdater(
    mapView: MapView,
    viewpointOperation: MapViewpointOperation?
) {
    LaunchedEffect(viewpointOperation) {
        viewpointOperation?.execute(mapView)
    }
}

@Composable
private fun CalloutUpdater(
    mapView: MapView,
    calloutPlacementOperation: CalloutPlacementOperation?
) {
    LaunchedEffect(calloutPlacementOperation) {
        calloutPlacementOperation?.execute(mapView)
    }
}

//@Composable
//private fun ShowCallout(
//    mapView: MapView,
//    callout: Callout,
////    calloutScreenCoordinate: ScreenCoordinate,
//    updateCalloutPosition: Boolean
//) {
//    val calloutScreenCoordinate: ScreenCoordinate = callout.let { mapView.locationToScreen(it.location) }
//    Box(
////        modifier = Modifier.offset(x = calloutScreenCoordinate.x.dp, y = calloutScreenCoordinate.y.dp)
////        modifier = Modifier.offset(x = 186.dp, y = 50.dp)
//        modifier = Modifier.offset(
//            x = with(LocalDensity.current) { calloutScreenCoordinate.x.toFloat().toDp() },
//            y = with(LocalDensity.current) { calloutScreenCoordinate.y.toFloat().toDp() })
//            .wrapContentSize()
////          .padding(30.dp)
//            .background(Color.White)
//            .border(
//                border = BorderStroke(2.dp, Color.LightGray),
//                shape = MaterialTheme.shapes.medium
//            )
//    )
//    {
//        callout.content?.invoke()
//    }
//}

/**
 * Sets up the callbacks for all the view-based [mapView] events.
 */
@Composable
private fun MapViewEventHandler(
    mapView: MapView,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)?,
    onVisibleAreaChanged: ((Polygon) -> Unit)?,
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
    onDrawStatusChanged: ((DrawStatus) -> Unit)?
) {
    val currentTimeExtentChanged by rememberUpdatedState(onTimeExtentChanged)
    val currentVisibleAreaChanged by rememberUpdatedState(onVisibleAreaChanged)
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
            mapView.viewpointChanged.collect {
                currentVisibleAreaChanged?.invoke(
                    mapView.visibleArea
                        ?: throw IllegalStateException("MapView visible area should not be null")
                )
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
    }
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
