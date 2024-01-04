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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SceneViewInteractionOptions
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A compose equivalent of the view-based [SceneView].
 *
 * @param modifier Modifier to be applied to the composable SceneView
 * @param arcGISScene the [ArcGISScene] to be rendered by this composable SceneView
 * @param viewpointOperation a [SceneViewpointOperation] that changes this SceneView to a new viewpoint
 * @param viewpointChangedState specifies lambdas invoked when the viewpoint of the composable SceneView has changed
 * @param graphicsOverlays the [GraphicsOverlayCollection] used by this composable SceneView
 * @param sceneViewProxy the [SceneViewProxy] to associate with the composable SceneView
 * @param sceneViewInteractionOptions the [SceneViewInteractionOptions] used by this composable SceneView
 * @param viewLabelProperties the [ViewLabelProperties] used by the composable SceneView
 * @param selectionProperties the [SelectionProperties] used by the composable SceneView
 * @param attributionState specifies the attribution bar's visibility, text changed and layout changed events
 * @param timeExtent the [TimeExtent] used by the composable SceneView
 * @param onTimeExtentChanged lambda invoked when the composable SceneView's [TimeExtent] is changed
 * @param onNavigationChanged lambda invoked when the navigation status of the composable SceneView has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the composable SceneView has changed
 * @param onLayerViewStateChanged lambda invoked when the composable SceneView's layer view state is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the composable SceneView
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint Camera of the composable SceneView has changed
 * @param onRotate lambda invoked when a user performs a rotation gesture on the composable SceneView
 * @param onScale lambda invoked when a user performs a pinch gesture on the composable SceneView
 * @param onUp lambda invoked when the user removes all their pointers from the composable SceneView
 * @param onDown lambda invoked when the user first presses on the composable SceneView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable SceneView
 * @param onDoubleTap lambda invoked the user double taps on the composable SceneView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable SceneView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable SceneView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable SceneView
 * @since 200.4.0
 */
@Composable
public fun SceneView(
    modifier: Modifier = Modifier,
    arcGISScene: ArcGISScene? = null,
    viewpointOperation: SceneViewpointOperation? = null,
    viewpointChangedState: ViewpointChangedState? = null,
    graphicsOverlays: GraphicsOverlayCollection = rememberGraphicsOverlayCollection(),
    sceneViewProxy: SceneViewProxy? = null,
    sceneViewInteractionOptions: SceneViewInteractionOptions = SceneViewInteractionOptions(),
    viewLabelProperties: ViewLabelProperties = ViewLabelProperties(),
    selectionProperties: SelectionProperties = SelectionProperties(),
    attributionState: AttributionState = AttributionState(),
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)? = null,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)? = null,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)? = null,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)? = null,
    onCurrentViewpointCameraChanged: ((camera: Camera) -> Unit)? = null,
    onRotate: ((RotationChangeEvent) -> Unit)? = null,
    onScale: ((ScaleChangeEvent) -> Unit)? = null,
    onUp: ((UpEvent) -> Unit)? = null,
    onDown: ((DownEvent) -> Unit)? = null,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)? = null,
    onDoubleTap: ((DoubleTapEvent) -> Unit)? = null,
    onLongPress: ((LongPressEvent) -> Unit)? = null,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)? = null,
    onPan: ((PanChangeEvent) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val sceneView = remember { SceneView(context) }

    AndroidView(
        modifier = modifier.semantics { contentDescription = "SceneView" },
        factory = { sceneView },
        update = {
            it.scene = arcGISScene
            it.interactionOptions = sceneViewInteractionOptions
            it.labeling = viewLabelProperties
            it.selectionProperties = selectionProperties
            it.setTimeExtent(timeExtent)
        })

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(sceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(sceneView)
            sceneView.onDestroy(lifecycleOwner)
        }
    }

    DisposableEffect(sceneViewProxy) {
        sceneViewProxy?.setSceneView(sceneView)
        onDispose {
            sceneViewProxy?.setSceneView(null)
        }
    }

    ViewpointUpdater(sceneView, viewpointOperation)

    GraphicsOverlaysUpdater(graphicsOverlays, sceneView)

    AttributionStateHandler(sceneView, attributionState)
    ViewpointChangedStateHandler(sceneView, viewpointChangedState)

    SceneViewEventHandler(
        sceneView,
        onTimeExtentChanged,
        onNavigationChanged,
        onSpatialReferenceChanged,
        onLayerViewStateChanged,
        onInteractingChanged,
        onCurrentViewpointCameraChanged,
        onRotate,
        onScale,
        onUp,
        onDown,
        onSingleTapConfirmed,
        onDoubleTap,
        onLongPress,
        onTwoPointerTap,
        onPan,
    )
}

/**
 * Updates the viewpoint of the provided view-based [sceneView] using the given [viewpointOperation]. This will be
 * recomposed when [viewpointOperation] changes.
 *
 * @since 200.4.0
 */
@Composable
private fun ViewpointUpdater(
    sceneView: SceneView,
    viewpointOperation: SceneViewpointOperation?
) {
    LaunchedEffect(viewpointOperation) {
        viewpointOperation?.execute(sceneView)
    }
}

/**
 * Sets up the callbacks for all the view-based [sceneView] events.
 */
@Composable
private fun SceneViewEventHandler(
    sceneView: SceneView,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)?,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)?,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)?,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)?,
    onCurrentViewpointCameraChanged: ((camera: Camera) -> Unit)?,
    onRotate: ((RotationChangeEvent) -> Unit)?,
    onScale: ((ScaleChangeEvent) -> Unit)?,
    onUp: ((UpEvent) -> Unit)?,
    onDown: ((DownEvent) -> Unit)?,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)?,
    onDoubleTap: ((DoubleTapEvent) -> Unit)?,
    onLongPress: ((LongPressEvent) -> Unit)?,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)?,
    onPan: ((PanChangeEvent) -> Unit)?,
) {
    val currentOnTimeExtentChanged by rememberUpdatedState(onTimeExtentChanged)
    val currentOnNavigationChanged by rememberUpdatedState(onNavigationChanged)
    val currentOnSpatialReferenceChanged by rememberUpdatedState(onSpatialReferenceChanged)
    val currentOnLayerViewStateChanged by rememberUpdatedState(onLayerViewStateChanged)
    val currentOnInteractingChanged by rememberUpdatedState(onInteractingChanged)
    val currentOnViewpointCameraChanged by rememberUpdatedState(onCurrentViewpointCameraChanged)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnUp by rememberUpdatedState(onUp)
    val currentOnDown by rememberUpdatedState(onDown)
    val currentSingleTapConfirmed by rememberUpdatedState(onSingleTapConfirmed)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTwoPointerTap by rememberUpdatedState(onTwoPointerTap)
    val currentOnPan by rememberUpdatedState(onPan)

    LaunchedEffect(Unit) {
        launch {
            sceneView.timeExtent.collect { currentTimeExtent ->
                currentOnTimeExtentChanged?.invoke(currentTimeExtent)
            }
        }
        launch {
            sceneView.navigationChanged.collect {
                currentOnNavigationChanged?.invoke(it)
            }
        }
        launch {
            sceneView.spatialReference.collect { spatialReference ->
                currentOnSpatialReferenceChanged?.invoke(spatialReference)
            }
        }
        launch {
            sceneView.layerViewStateChanged.collect { currentLayerViewState ->
                currentOnLayerViewStateChanged?.invoke(currentLayerViewState)
            }
        }
        launch {
            sceneView.viewpointChanged.collect {
                currentOnViewpointCameraChanged?.invoke(sceneView.getCurrentViewpointCamera())
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.isInteracting.collect { isInteracting ->
                currentOnInteractingChanged?.invoke(isInteracting)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onRotate.collect { rotationChangeEvent ->
                currentOnRotate?.invoke(rotationChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onScale.collect { scaleChangeEvent ->
                currentOnScale?.invoke(scaleChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onUp.collect { upEvent ->
                currentOnUp?.invoke(upEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onDown.collect { downEvent ->
                currentOnDown?.invoke(downEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onSingleTapConfirmed.collect { singleTapConfirmedEvent ->
                currentSingleTapConfirmed?.invoke(singleTapConfirmedEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onDoubleTap.collect { doubleTapEvent ->
                currentOnDoubleTap?.invoke(doubleTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onLongPress.collect { longPressEvent ->
                currentOnLongPress?.invoke(longPressEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onTwoPointerTap.collect { twoPointerTapEvent ->
                currentOnTwoPointerTap?.invoke(twoPointerTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onPan.collect { panChangeEvent ->
                currentOnPan?.invoke(panChangeEvent)
            }
        }
    }
}
