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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.CameraController
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.GlobeCameraController
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ImageOverlay
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SceneViewInteractionOptions
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * A compose equivalent of the view-based [SceneView].
 *
 * @param modifier Modifier to be applied to the composable SceneView
 * @param arcGISScene the [ArcGISScene] to be rendered by this composable SceneView
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale]
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry]
 * @param graphicsOverlays graphics overlays used by this composable SceneView
 * @param sceneViewProxy the [SceneViewProxy] to associate with the composable SceneView
 * @param sceneViewInteractionOptions the [SceneViewInteractionOptions] used by this composable SceneView
 * @param viewLabelProperties the [ViewLabelProperties] used by the composable SceneView
 * @param selectionProperties the [SelectionProperties] used by the composable SceneView
 * @param isAttributionBarVisible true if attribution bar is visible in the composable SceneView, false otherwise
 * @param onAttributionTextChanged lambda invoked when the attribution text of the composable SceneView has changed
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes
 * @param cameraController the [CameraController] to manage the position, orientation, and movement of the camera
 * @param analysisOverlays analysis overlays that render the results of 3D visual analysis on the composable SceneView
 * @param imageOverlays image overlays for displaying images in the composable SceneView
 * @param atmosphereEffect the effect applied to the scene's atmosphere
 * @param timeExtent the [TimeExtent] used by the composable SceneView
 * @param onTimeExtentChanged lambda invoked when the composable SceneView's [TimeExtent] is changed
 * @param spaceEffect the visual effect of outer space in the composable SceneView
 * @param sunTime the position of the sun in the composable SceneView based on a specific date and time
 * @param sunLighting the type of ambient sunlight and shadows in the composable SceneView
 * @param ambientLightColor the color of the composable SceneView's ambient light
 * @param onNavigationChanged lambda invoked when the navigation status of the composable SceneView has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the composable SceneView has changed
 * @param onLayerViewStateChanged lambda invoked when the composable SceneView's layer view state is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the composable SceneView
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the composable SceneView has changed
 * @param onRotate lambda invoked when a user performs a rotation gesture on the composable SceneView
 * @param onScale lambda invoked when a user performs a pinch gesture on the composable SceneView
 * @param onUp lambda invoked when the user removes all their pointers from the composable SceneView
 * @param onDown lambda invoked when the user first presses on the composable SceneView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable SceneView
 * @param onDoubleTap lambda invoked the user double taps on the composable SceneView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable SceneView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable SceneView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable SceneView
 * @param onDrawStatusChanged lambda invoked when the draw status of the composable SceneView is changed
 * @sample com.arcgismaps.toolkit.geocompose.samples.SceneViewSample
 * @see
 * - <a href="https://developers.arcgis.com/kotlin/scenes-3d/tutorials/display-a-scene/">Display a scene tutorial</a>
 * - <a href="https://developers.arcgis.com/kotlin/scenes-3d/tutorials/display-a-web-scene/">Display a web scene tutorial</a>
 * @since 200.4.0
 */
@Composable
public fun SceneView(
    modifier: Modifier = Modifier,
    arcGISScene: ArcGISScene? = null,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    sceneViewProxy: SceneViewProxy? = null,
    sceneViewInteractionOptions: SceneViewInteractionOptions = SceneViewInteractionOptions(),
    viewLabelProperties: ViewLabelProperties = ViewLabelProperties(),
    selectionProperties: SelectionProperties = SelectionProperties(),
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
    cameraController: CameraController = GlobeCameraController(),
    analysisOverlays: List<AnalysisOverlay> = remember { emptyList() },
    imageOverlays: List<ImageOverlay> = remember { emptyList() },
    atmosphereEffect: AtmosphereEffect = AtmosphereEffect.HorizonOnly,
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    spaceEffect: SpaceEffect = SpaceEffect.Stars,
    sunTime: Instant = Instant.parse("2000-09-22T12:00:00Z"),
    sunLighting: LightingMode = LightingMode.NoLight,
    ambientLightColor: Color = Color(220, 220, 220, 255),
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
    onDrawStatusChanged: ((DrawStatus) -> Unit)? = null
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
            it.cameraController = cameraController
            it.atmosphereEffect = atmosphereEffect
            it.spaceEffect = spaceEffect
            it.sunTime = sunTime
            it.sunLighting = sunLighting
            it.ambientLightColor = com.arcgismaps.Color(ambientLightColor.toArgb())
            it.isAttributionBarVisible = isAttributionBarVisible
            if (it.graphicsOverlays != graphicsOverlays) {
                it.graphicsOverlays.apply {
                    clear()
                    addAll(graphicsOverlays)
                }
            }
            if (sceneView.analysisOverlays != analysisOverlays) {
                it.analysisOverlays.apply {
                    clear()
                    addAll(analysisOverlays)
                }
            }
            if (sceneView.imageOverlays != imageOverlays) {
                it.imageOverlays.apply {
                    clear()
                    addAll(imageOverlays)
                }
            }
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

    SceneViewEventHandler(
        sceneView,
        onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry,
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
        onDrawStatusChanged,
        onAttributionTextChanged,
        onAttributionBarLayoutChanged
    )
}

/**
 * Sets up the callbacks for all the view-based [sceneView] events.
 */
@Composable
private fun SceneViewEventHandler(
    sceneView: SceneView,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)?,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)?,
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
    onDrawStatusChanged: ((DrawStatus) -> Unit)?,
    onAttributionTextChanged: ((String) -> Unit)?,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)?,
) {
    val currentOnViewpointChangedForCenterAndScale by rememberUpdatedState(
        onViewpointChangedForCenterAndScale
    )
    val currentOnViewpointChangedForBoundingGeometry by rememberUpdatedState(
        onViewpointChangedForBoundingGeometry
    )
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
    val currentOnDrawStatusChanged by rememberUpdatedState(onDrawStatusChanged)
    val currentOnAttributionTextChanged by rememberUpdatedState(onAttributionTextChanged)
    val currentOnAttributionBarLayoutChanged by rememberUpdatedState(onAttributionBarLayoutChanged)

    LaunchedEffect(Unit) {
        launch {
            sceneView.viewpointChanged.collect {
                currentOnViewpointChangedForCenterAndScale?.let { callback ->
                    sceneView.getCurrentViewpoint(ViewpointType.CenterAndScale)?.let(callback)
                }
                currentOnViewpointChangedForBoundingGeometry?.let { callback ->
                    sceneView.getCurrentViewpoint(ViewpointType.BoundingGeometry)?.let(callback)
                }
            }
        }
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
        launch {
            sceneView.drawStatus.collect { drawStatus ->
                currentOnDrawStatusChanged?.invoke(drawStatus)
            }
        }
        launch {
            sceneView.attributionText.collect { attributionText ->
                currentOnAttributionTextChanged?.invoke(attributionText)
            }
        }
        launch {
            sceneView.onAttributionBarLayoutChanged.collect { attributionBarLayoutChangeEvent ->
                currentOnAttributionBarLayoutChanged?.invoke(attributionBarLayoutChangeEvent)
            }
        }
    }
}
