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

package com.arcgismaps.toolkit.ar

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ImageOverlay
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SceneViewInteractionOptions
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.CalibrationState
import com.arcgismaps.toolkit.ar.internal.GeospatialTrackingCameraController
import com.arcgismaps.toolkit.ar.internal.WorldScaleCameraController
import com.arcgismaps.toolkit.ar.internal.WorldTrackingCameraController
import com.arcgismaps.toolkit.ar.internal.rememberArCoreInstalled
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberPeDataConfigured
import com.arcgismaps.toolkit.ar.internal.rememberPermissionsGranted
import com.arcgismaps.toolkit.ar.internal.setFieldOfViewFromLensIntrinsics
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import java.time.Instant

/**
 * A scene view that provides a world-scale augmented reality experience.
 *
 * Note: You must follow [Google's user privacy requirements for ARCore](https://developers.google.com/ar/develop/privacy-requirements)
 * when using WorldScaleSceneView in your application.
 *
 * @param arcGISScene the [ArcGISScene] to be rendered by this WorldScaleSceneView.
 * @param modifier Modifier to be applied to the WorldScaleSceneView.
 * @param worldScaleTrackingMode the type of tracking configuration used by the WorldScaleSceneView.
 * Determines how the position and orientation of the device is obtained and synchronized with
 * the scene view's camera.
 * @param clippingDistance the clipping distance in meters around the scene view's camera. A null value
 * means that no data will be clipped.
 * @param onInitializationStatusChanged lambda invoked when the initialization status
 * of this WorldScaleSceneView changes.
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a
 * viewpoint type of [ViewpointType.CenterAndScale].
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a
 * viewpoint type of [ViewpointType.BoundingGeometry].
 * @param graphicsOverlays graphics overlays used by the WorldScaleSceneView.
 * @param worldScaleSceneViewProxy the [WorldScaleSceneViewProxy] to associate with the
 * WorldScaleSceneView.
 * @param viewLabelProperties the [ViewLabelProperties] used by the WorldScaleSceneView.
 * @param selectionProperties the [SelectionProperties] used by the WorldScaleSceneView.
 * @param isAttributionBarVisible true if attribution bar is visible in the WorldScaleSceneView,
 * false otherwise.
 * @param onAttributionTextChanged lambda invoked when the attribution text of the
 * WorldScaleSceneView has changed.
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size
 * changes.
 * @param analysisOverlays analysis overlays that render the results of 3D visual analysis on the
 * WorldScaleSceneView.
 * @param imageOverlays image overlays for displaying images in the WorldScaleSceneView.
 * @param timeExtent the [TimeExtent] used by the WorldScaleSceneView.
 * @param onTimeExtentChanged lambda invoked when the WorldScaleSceneView's [TimeExtent] is changed.
 * @param sunTime the position of the sun in the WorldScaleSceneView based on a specific date and
 * time.
 * @param sunLighting the type of ambient sunlight and shadows in the WorldScaleSceneView.
 * @param ambientLightColor the color of the WorldScaleSceneView's ambient light.
 * @param onNavigationChanged lambda invoked when the navigation status of the WorldScaleSceneView
 * has changed.
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the
 * WorldScaleSceneView has changed.
 * @param onLayerViewStateChanged lambda invoked when the WorldScaleSceneView's layer view state is
 * changed.
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the
 * WorldScaleSceneView.
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the
 * WorldScaleSceneView has changed.
 * @param onRotate lambda invoked when a user performs a rotation gesture on the WorldScaleSceneView.
 * @param onScale lambda invoked when a user performs a pinch gesture on the WorldScaleSceneView.
 * @param onUp lambda invoked when the user removes all their pointers from the WorldScaleSceneView.
 * @param onDown lambda invoked when the user first presses on the WorldScaleSceneView.
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the WorldScaleSceneView.
 * @param onDoubleTap lambda invoked the user double taps on the WorldScaleSceneView.
 * @param onLongPress lambda invoked when a user holds a pointer on the WorldScaleSceneView.
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the WorldScaleSceneView.
 * @param onPan lambda invoked when a user drags a pointer or pointers across WorldScaleSceneView.
 * @param content the content of the WorldScaleSceneView.
 *
 * @since 200.7.0
 */
@Composable
public fun WorldScaleSceneView(
    arcGISScene: ArcGISScene,
    modifier: Modifier = Modifier,
    worldScaleTrackingMode: WorldScaleTrackingMode = remember { WorldScaleTrackingMode.World() },
    clippingDistance: Double? = null,
    onInitializationStatusChanged: ((WorldScaleSceneViewStatus) -> Unit)? = null,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    worldScaleSceneViewProxy: WorldScaleSceneViewProxy = remember { WorldScaleSceneViewProxy() },
    viewLabelProperties: ViewLabelProperties = remember { ViewLabelProperties() },
    selectionProperties: SelectionProperties = remember { SelectionProperties() },
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
    analysisOverlays: List<AnalysisOverlay> = remember { emptyList() },
    imageOverlays: List<ImageOverlay> = remember { emptyList() },
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    sunTime: Instant = SceneViewDefaults.DefaultSunTime,
    sunLighting: LightingMode = LightingMode.NoLight,
    ambientLightColor: Color = SceneViewDefaults.DefaultAmbientLightColor,
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
    content: (@Composable WorldScaleSceneViewScope.() -> Unit)? = null
) {
    val initializationStatus = rememberWorldScaleSceneViewStatus()

    val arCoreInstalled by rememberArCoreInstalled(
        onFailed = {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        }
    )
    // If ARCore is not installed, we can't display anything
    if (!arCoreInstalled) return

    val allPermissionsGranted by rememberPermissionsGranted(
        permissionsToRequest = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        ),
        onFailed = {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        }
    )
    // If we don't have permission for camera or location, we can't display anything
    if (!allPermissionsGranted) return

    val pedataConfigured by rememberPeDataConfigured(
        onFailed = {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        }
    )
    // If PE data could not be configured, we can't position the scene camera accurately
    if (!pedataConfigured) return

    val arSessionWrapper =
        rememberArSessionWrapper(
            applicationContext = LocalContext.current.applicationContext,
            onError = {
                initializationStatus.update(
                    WorldScaleSceneViewStatus.FailedToInitialize(it),
                    onInitializationStatusChanged
                )
            },
            useGeospatial = worldScaleTrackingMode is WorldScaleTrackingMode.Geospatial
        )

    val calibrationState = remember { CalibrationState() }

    DisposableEffect(Unit) {
        worldScaleSceneViewProxy.setSessionWrapper(arSessionWrapper)
        onDispose {
            worldScaleSceneViewProxy.setSessionWrapper(null)
        }
    }

    val worldScaleCameraController: WorldScaleCameraController by rememberWorldScaleCameraController(
        context = LocalContext.current,
        worldScaleTrackingMode = worldScaleTrackingMode,
        calibrationState = calibrationState,
        clippingDistance = clippingDistance,
        onUpdateInitializationStatus = {
            initializationStatus.update(
                it,
                onInitializationStatusChanged
            )
        },
        onResetOriginCamera = {
            // onResetOriginCamera is only called in WorldTracking mode. We need to reset the AR
            // session in WorldTracking Mode when the origin camera is reset, because AR tracking
            // will need to start from scratch.
            arSessionWrapper.resetSession(worldScaleTrackingMode is WorldScaleTrackingMode.Geospatial)
        }
    )

    Box(modifier = modifier) {
        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation, session ->
                worldScaleCameraController.updateCamera(frame, session)
                if (!worldScaleCameraController.hasSetOriginCamera) return@ArCameraFeed
                worldScaleSceneViewProxy.sceneViewProxy.setFieldOfViewFromLensIntrinsics(
                    frame.camera,
                    displayRotation
                )
                worldScaleSceneViewProxy.sceneViewProxy.renderFrame()
            },
            onTapWithHitResult = { },
            onFirstPlaneDetected = { },
            visualizePlanes = false
        )
        // Don't display the scene view if the camera has not been set up yet, or else a globe will appear
        if (!worldScaleCameraController.hasSetOriginCamera) return@WorldScaleSceneView
        // Once the origin camera is set, we can say we're initialized
        initializationStatus.update(
            WorldScaleSceneViewStatus.Initialized,
            onInitializationStatusChanged
        )
        SceneView(
            arcGISScene = arcGISScene,
            modifier = Modifier.fillMaxSize(),
            onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
            onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
            graphicsOverlays = graphicsOverlays,
            sceneViewProxy = worldScaleSceneViewProxy.sceneViewProxy,
            sceneViewInteractionOptions = remember {
                // Disable interaction, which is not supported in WorldScale scenarios
                SceneViewInteractionOptions().apply {
                    isEnabled = false
                }
            },
            viewLabelProperties = viewLabelProperties,
            selectionProperties = selectionProperties,
            isAttributionBarVisible = isAttributionBarVisible,
            onAttributionTextChanged = onAttributionTextChanged,
            onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
            cameraController = worldScaleCameraController.cameraController,
            analysisOverlays = analysisOverlays,
            imageOverlays = imageOverlays,
            atmosphereEffect = AtmosphereEffect.None,
            timeExtent = timeExtent,
            onTimeExtentChanged = onTimeExtentChanged,
            spaceEffect = SpaceEffect.Transparent,
            sunTime = sunTime,
            sunLighting = sunLighting,
            ambientLightColor = ambientLightColor,
            onNavigationChanged = onNavigationChanged,
            onSpatialReferenceChanged = onSpatialReferenceChanged,
            onLayerViewStateChanged = onLayerViewStateChanged,
            onInteractingChanged = onInteractingChanged,
            onCurrentViewpointCameraChanged = {
                worldScaleSceneViewProxy.setCurrentCamera(it)
                onCurrentViewpointCameraChanged?.invoke(it)
            },
            onRotate = onRotate,
            onScale = onScale,
            onUp = onUp,
            onDown = onDown,
            onSingleTapConfirmed = onSingleTapConfirmed,
            onDoubleTap = onDoubleTap,
            onLongPress = onLongPress,
            onTwoPointerTap = onTwoPointerTap,
            onPan = onPan,
            content = {
                content?.let { content ->
                    val worldScaleSceneViewScope = remember {
                        WorldScaleSceneViewScope(
                            sceneViewScope = this,
                            calibrationState = calibrationState
                        )
                    }
                    content.invoke(worldScaleSceneViewScope)
                }
            }
        )
    }
}

/**
 * Creates and remembers the updated state of a [WorldScaleCameraController].
 *
 * @param context The context used to create the camera controller.
 * @param worldScaleTrackingMode The tracking mode used by the camera controller. If this parameter
 * changes, the camera controller will be recreated.
 * @param calibrationState The calibration state used by the camera controller.
 * @param clippingDistance The clipping distance used by the camera controller.
 * @param onUpdateInitializationStatus The callback used to update the initialization status of the [WorldScaleSceneView]
 * @param onResetOriginCamera Called when the [WorldTrackingCameraController]'s origin camera is set
 *
 * @since 200.7.0
 */
@Composable
internal fun rememberWorldScaleCameraController(
    context: Context,
    worldScaleTrackingMode: WorldScaleTrackingMode,
    calibrationState: CalibrationState,
    clippingDistance: Double?,
    onUpdateInitializationStatus: (WorldScaleSceneViewStatus) -> Unit,
    onResetOriginCamera: () -> Unit
): State<WorldScaleCameraController> {
    val worldScaleCameraController = remember(worldScaleTrackingMode) {
        when (worldScaleTrackingMode) {
            is WorldScaleTrackingMode.Geospatial -> {
                GeospatialTrackingCameraController(
                    calibrationState = calibrationState,
                    clippingDistance = clippingDistance,
                    context = context,
                    onError = {
                        onUpdateInitializationStatus(WorldScaleSceneViewStatus.FailedToInitialize(it))
                    }
                )
            }

            is WorldScaleTrackingMode.World -> {
                ArcGISEnvironment.applicationContext = context.applicationContext
                WorldTrackingCameraController(
                    calibrationState = calibrationState,
                    clippingDistance = clippingDistance,
                    onLocationDataSourceFailedToStart = { it: Throwable ->
                        onUpdateInitializationStatus(WorldScaleSceneViewStatus.FailedToInitialize(it))
                    },
                    onResetOriginCamera = onResetOriginCamera
                )
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(worldScaleCameraController) {
        lifecycleOwner.lifecycle.addObserver(worldScaleCameraController)
        onUpdateInitializationStatus(WorldScaleSceneViewStatus.Initializing)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(worldScaleCameraController)
            (worldScaleCameraController).onDestroy(lifecycleOwner)
        }
    }
    return rememberUpdatedState(worldScaleCameraController)
}

