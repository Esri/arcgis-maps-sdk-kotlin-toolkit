/*
 *
 *  Copyright 2025 Esri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.LocalSceneView
import com.arcgismaps.mapping.view.LocalSceneViewInteractionOptions
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A compose equivalent of the view-based [LocalSceneView].
 *
 * @since 300.0.0
 */
@Composable
public fun LocalSceneView(
    scene: ArcGISScene,
    modifier: Modifier = Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    interactionOptions: LocalSceneViewInteractionOptions = remember { LocalSceneViewInteractionOptions() },
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
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
    onDrawStatusChanged: ((DrawStatus) -> Unit)? = null,
    content: (@Composable LocalSceneViewScope.() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val localSceneView = remember { LocalSceneView(context) }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "LocalSceneView" },
            factory = { localSceneView },
            update = {
                it.scene = scene
                it.interactionOptions = interactionOptions
            }
        )

        val localSceneViewScope = remember { LocalSceneViewScope(localSceneView) }
        val isLocalSceneViewReady = localSceneView.rememberIsReady()

        // Invoke the content lambda only when the SceneView is ready
        if (isLocalSceneViewReady.value) {
            content?.let {
                localSceneViewScope.it()
            }
        }
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(localSceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(localSceneView)
            localSceneView.onDestroy(lifecycleOwner)
        }
    }

    LocalSceneViewEventHandler(
        localSceneView,
        onTimeExtentChanged,
        onNavigationChanged,
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
        localSceneView = localSceneView,
        onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
        onCurrentViewpointCameraChanged = onCurrentViewpointCameraChanged
    )
}

/**
 * Sets up the callbacks for all the view-based [LocalSceneView] events.
 *
 * @since 300.0.0
 */
@Composable
private fun LocalSceneViewEventHandler(
    localSceneView: LocalSceneView,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)?,
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
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)?,
) {
    val currentOnTimeExtentChanged by rememberUpdatedState(onTimeExtentChanged)
    val currentOnNavigationChanged by rememberUpdatedState(onNavigationChanged)
    val currentOnSpatialReferenceChanged by rememberUpdatedState(onSpatialReferenceChanged)
    val currentOnLayerViewStateChanged by rememberUpdatedState(onLayerViewStateChanged)
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
    val currentOnAttributionTextChanged by rememberUpdatedState(onAttributionTextChanged)
    val currentOnAttributionBarLayoutChanged by rememberUpdatedState(onAttributionBarLayoutChanged)

    LaunchedEffect(Unit) {
        launch {
            localSceneView.timeExtent.collect { currentTimeExtent ->
                currentOnTimeExtentChanged?.invoke(currentTimeExtent)
            }
        }
        launch {
            localSceneView.navigationChanged.collect {
                currentOnNavigationChanged?.invoke(it)
            }
        }
        launch {
            localSceneView.spatialReference.collect { spatialReference ->
                currentOnSpatialReferenceChanged?.invoke(spatialReference)
            }
        }
        launch {
            localSceneView.layerViewStateChanged.collect { currentLayerViewState ->
                currentOnLayerViewStateChanged?.invoke(currentLayerViewState)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.isInteracting.collect { isInteracting ->
                currentOnInteractingChanged?.invoke(isInteracting)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onRotate.collect { rotationChangeEvent ->
                currentOnRotate?.invoke(rotationChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onScale.collect { scaleChangeEvent ->
                currentOnScale?.invoke(scaleChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onUp.collect { upEvent ->
                currentOnUp?.invoke(upEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onDown.collect { downEvent ->
                currentOnDown?.invoke(downEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onSingleTapConfirmed.collect { singleTapConfirmedEvent ->
                currentSingleTapConfirmed?.invoke(singleTapConfirmedEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onDoubleTap.collect { doubleTapEvent ->
                currentOnDoubleTap?.invoke(doubleTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onLongPress.collect { longPressEvent ->
                currentOnLongPress?.invoke(longPressEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onTwoPointerTap.collect { twoPointerTapEvent ->
                currentOnTwoPointerTap?.invoke(twoPointerTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            localSceneView.onPan.collect { panChangeEvent ->
                currentOnPan?.invoke(panChangeEvent)
            }
        }
        launch {
            localSceneView.drawStatus.collect { drawStatus ->
                currentOnDrawStatusChanged?.invoke(drawStatus)
            }
        }
        launch {
            localSceneView.attributionText.collect { attributionText ->
                currentOnAttributionTextChanged?.invoke(attributionText)
            }
        }
        launch {
            localSceneView.onAttributionBarLayoutChanged.collect { attributionBarLayoutChangeEvent ->
                currentOnAttributionBarLayoutChanged?.invoke(attributionBarLayoutChangeEvent)
            }
        }
    }
}

/**
 * Handles viewpoint change events and persistence for a [LocalSceneView].
 *
 * @since 300.0.0
 */
@Composable
private fun ViewpointHandler(
    localSceneView: LocalSceneView,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)?,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)?,
    onCurrentViewpointCameraChanged: ((camera: Camera) -> Unit)?
) {
    val currentOnViewpointChangedForCenterAndScale by rememberUpdatedState(
        onViewpointChangedForCenterAndScale
    )
    val currentOnViewpointChangedForBoundingGeometry by rememberUpdatedState(
        onViewpointChangedForBoundingGeometry
    )
    val currentOnCurrentViewpointCameraChanged by rememberUpdatedState(
        onCurrentViewpointCameraChanged
    )

    var persistedCamera by rememberSaveable(
        saver = Saver(
            save = {
                val camera = it.value ?: return@Saver null
                listOf(
                    camera.transformationMatrix.quaternionX,
                    camera.transformationMatrix.quaternionY,
                    camera.transformationMatrix.quaternionZ,
                    camera.transformationMatrix.quaternionW,
                    camera.transformationMatrix.translationX,
                    camera.transformationMatrix.translationY,
                    camera.transformationMatrix.translationZ
                )
            },
            restore = { matrixValues ->
                if (matrixValues.isEmpty()) return@Saver mutableStateOf(null)
                val camera = Camera(
                    TransformationMatrix.createWithQuaternionAndTranslation(
                        quaternionX = matrixValues[0],
                        quaternionY = matrixValues[1],
                        quaternionZ = matrixValues[2],
                        quaternionW = matrixValues[3],
                        translationX = matrixValues[4],
                        translationY = matrixValues[5],
                        translationZ = matrixValues[6]
                    )
                )
                mutableStateOf(camera)
            }
        )
    ) {
        mutableStateOf<Camera?>(null)
    }

    LaunchedEffect(Unit) {
        // if there is a persisted viewpoint, restore it when the SceneView enters the composition
        persistedCamera?.let { localSceneView.setViewpoint(it) }
        launch {
            localSceneView.viewpointChanged.collect {
                val currentViewpointCamera = localSceneView.getCurrentViewpointCamera()
                persistedCamera = currentViewpointCamera
                currentOnCurrentViewpointCameraChanged?.invoke(currentViewpointCamera)
                currentOnViewpointChangedForCenterAndScale?.let { callback ->
                    val currentViewpoint =
                        localSceneView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                    currentViewpoint?.let(callback)
                }
                currentOnViewpointChangedForBoundingGeometry?.let { callback ->
                    val currentViewpoint =
                        localSceneView.getCurrentViewpoint(ViewpointType.BoundingGeometry)
                    currentViewpoint?.let(callback)
                }
            }
        }
    }
}

/**
 * The receiver class of the [LocalSceneView] content lambda.
 *
 * @since 300.0.0
 */
public class LocalSceneViewScope internal constructor(localSceneView: LocalSceneView) :
    GeoViewScope(localSceneView)