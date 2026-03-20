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

import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.floor.FloorAware
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
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.toolkit.geoviewcompose.internal.GeoViewA11yCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A user interface control that displays geographic content defined by a local [ArcGISScene].
 * A local scene view is a user interface that displays layers and graphics. It controls the area of
 * the local scene that is visible and supports user interactions such as pan and zoom. The local
 * scene view also provides access to the underlying layer data in a local scene.
 *
 * To display a local scene, add this view to the composition and pass a local `ArcGISScene` to it
 * via the [scene] parameter. This loads the scene and its content, such as a [Basemap] and a
 * collection of operational layers, and displays this content on screen.
 *
 * User interactions such as pan, zoom, rotate, and identify or selection are supported in the
 * [LocalSceneView] using touch interaction.
 *
 * The visible area ([Viewpoint]) of the local scene view is defined by the visible extent of the
 * scene. To determine the current visible area or the center point and scale of the view, make sure
 * that any user-initiated or programmatic navigation is complete before getting the current
 * [Viewpoint] by listening to the [onNavigationChanged] callback.
 *
 * You can programmatically set the visible area by specifying a viewpoint. For example,
 * [GeoViewProxy.setViewpoint] sets the visible area to the extent of a provided geometry, and
 * [LocalSceneViewProxy.setViewpointCamera] centers the local scene view at a given point. Any
 * geometries passed to these methods are automatically projected to match the [SpatialReference] of
 * the [scene], if required.
 *
 * [FloorAware] is not yet supported in the local scene view, and any layer [FloorAware] configurations
 * will not be honored.
 *
 * In an MVC architecture, the local scene view represents the View tier. The Model tier is
 * represented by the [ArcGISScene] object which can provide a collection of operational layers and
 * a [Basemap]. You can only set one local scene per local scene view, but you can replace the
 * [scene] with another when the application is running.
 *
 * @param scene the [ArcGISScene] to be rendered by this composable LocalSceneView
 * @param modifier modifier to be applied to the composable LocalSceneView
 * @param localSceneViewProxy the [LocalSceneViewProxy] to associate with the composable LocalSceneView
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a
 * viewpoint type of [ViewpointType.CenterAndScale]
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a
 * viewpoint type of [ViewpointType.BoundingGeometry]
 * @param interactionOptions the [LocalSceneViewInteractionOptions] used by this composable LocalSceneView
 * @param selectionProperties the [SelectionProperties] used by the composable LocalSceneView
 * @param isAttributionBarVisible true if attribution bar is visible in the composable LocalSceneView,
 * false otherwise
 * @param onAttributionTextChanged lambda invoked when the attribution text of the composable
 * LocalSceneView has changed
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size
 * changes
 * @param onNavigationChanged lambda invoked when the navigation status of the composable LocalSceneView
 * has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the composable
 * LocalSceneView has changed
 * @param onLayerViewStateChanged lambda invoked when the composable LocalSceneView's layer view state
 * is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the
 * composable LocalSceneView
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the composable
 * LocalSceneView has changed
 * @param onRotate lambda invoked when a user performs a rotation gesture on the composable LocalSceneView
 * @param onScale lambda invoked when a user performs a pinch gesture on the composable LocalSceneView
 * @param onUp lambda invoked when the user removes all their pointers from the composable LocalSceneView
 * @param onDown lambda invoked when the user first presses on the composable LocalSceneView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable LocalSceneView
 * @param onDoubleTap lambda invoked the user double taps on the composable LocalSceneView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable LocalSceneView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable LocalSceneView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable LocalSceneView
 * @param onDrawStatusChanged lambda invoked when the draw status of the composable LocalSceneView
 * is changed
 * @param canFocus pass true if the LocalSceneView should receive focus. Note that specifying a modifier property `Modifier.focusProperties { canFocus = true/false }` on the LocalSceneView composable has no effect.
 * @param content the content of the composable LocalSceneView
 *
 * @since 300.0.0
 */
@Composable
public fun LocalSceneView(
    scene: ArcGISScene,
    modifier: Modifier = Modifier,
    localSceneViewProxy: LocalSceneViewProxy? = null,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    interactionOptions: LocalSceneViewInteractionOptions = remember { LocalSceneViewInteractionOptions() },
    selectionProperties: SelectionProperties = remember { SelectionProperties() },
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
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
    canFocus: Boolean = true,
    content: (@Composable LocalSceneViewScope.() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val localSceneView = remember {
        LocalSceneView(context)
    }
    val contentFocusRequester = remember(canFocus) { FocusRequester() }
    val a11yCoordinator = remember { GeoViewA11yCoordinator(localSceneView, canFocus, contentFocusRequester) }
    Box(modifier = modifier.clipToBounds()) {
        // kotlin 2.3.0 bug https://youtrack.jetbrains.com/projects/CMP/issues/CMP-8600/Calling-a-androidx.compose.ui.UiComposable-composable-function-where-a-UI-Composable-composable-was-expected-with-some
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .focusable(a11yCoordinator.isGeoViewFocusable)
                .focusProperties { next = contentFocusRequester }
                .semantics { contentDescription = "LocalSceneView" },
            factory = { localSceneView },
            update = {
                it.scene = scene
                it.isFocusable = a11yCoordinator.isGeoViewFocusable
                it.interactionOptions = interactionOptions
                it.isAttributionBarVisible = isAttributionBarVisible
                it.selectionProperties = selectionProperties
            }
        )

        val localSceneViewScope = remember { LocalSceneViewScope(localSceneView, a11yCoordinator) }
        val isLocalSceneViewReady = localSceneView.rememberIsReady()

        // Invoke the content lambda only when the LocalSceneView is ready
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

    DisposableEffect(localSceneViewProxy) {
        localSceneViewProxy?.setLocalSceneView(localSceneView)
        onDispose {
            localSceneViewProxy?.setLocalSceneView(null)
        }
    }

    LocalSceneViewEventHandler(
        localSceneView,
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
        localSceneView,
        onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry,
        onCurrentViewpointCameraChanged
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
        // if there is a persisted viewpoint, restore it when the LocalSceneView enters the composition
        persistedViewpoint?.let { localSceneView.setViewpoint(it) }
        launch {
            localSceneView.viewpointChanged.collect {
                val currentViewpointCenterAndScale =
                    localSceneView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                persistedViewpoint = currentViewpointCenterAndScale
                currentOnCurrentViewpointCameraChanged?.invoke(localSceneView.getCurrentViewpointCamera())
                currentOnViewpointChangedForCenterAndScale?.let { callback ->
                    currentViewpointCenterAndScale?.let(callback)
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
public class LocalSceneViewScope internal constructor(
    localSceneView: LocalSceneView, a11yCoordinator: GeoViewA11yCoordinator
) : GeoViewScope(localSceneView, a11yCoordinator)
