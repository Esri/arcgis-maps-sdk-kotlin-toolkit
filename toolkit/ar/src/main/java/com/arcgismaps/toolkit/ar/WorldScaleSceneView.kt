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
    if (!arCoreInstalled) return@WorldScaleSceneView

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
    if (!allPermissionsGranted) return@WorldScaleSceneView

    val pedataConfigured by rememberPeDataConfigured(
        onFailed = {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        }
    )
    // If PE data could not be configured, we can't position the scene camera accurately
    if (!pedataConfigured) return@WorldScaleSceneView

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
            onCurrentViewpointCameraChanged = onCurrentViewpointCameraChanged,
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
                    context = context
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

