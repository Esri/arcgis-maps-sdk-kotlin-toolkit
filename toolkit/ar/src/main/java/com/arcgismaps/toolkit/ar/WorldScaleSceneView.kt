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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.geometry.TransformationCatalog
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
import com.arcgismaps.toolkit.ar.internal.PeData
import com.arcgismaps.toolkit.ar.internal.rememberArCoreInstalled
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberPermissionsGranted
import com.arcgismaps.toolkit.ar.internal.rememberWorldTrackingCameraController
import com.arcgismaps.toolkit.ar.internal.setFieldOfViewFromLensIntrinsics
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
public fun WorldScaleSceneView(
    arcGISScene: ArcGISScene,
    modifier: Modifier = Modifier,
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
        rememberArSessionWrapper(applicationContext = LocalContext.current.applicationContext)


    val localLifecycleOwner = LocalLifecycleOwner.current
    val calibrationState = remember { CalibrationState() }

    val locationTracker = rememberWorldTrackingCameraController(
        calibrationState = calibrationState,
        clippingDistance = clippingDistance,
        onLocationDataSourceFailedToStart = {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        },
        onResetOriginCamera = {
            localLifecycleOwner.lifecycleScope.launch {
                arSessionWrapper.resetSession()
            }
        }
    )

    Box(modifier = modifier) {
        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation ->
                locationTracker.updateCamera(frame)
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
        // Once the session is created, we can say we're initialized
        initializationStatus.update(
            WorldScaleSceneViewStatus.Initialized,
            onInitializationStatusChanged
        )
        // Don't display the scene view if the camera has not been set up yet, or else a globe will appear
        if (!locationTracker.hasSetOriginCamera) return@WorldScaleSceneView
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
            cameraController = locationTracker.cameraController,
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
 * Deploys the PE data file to the device's external files directory and configures
 * [TransformationCatalog.projectionEngineDirectory] with its path.
 *
 * @since 200.7.0
 */
@Composable
private fun rememberPeDataConfigured(
    onFailed: (Throwable) -> Unit
): State<Boolean> {
    val context = LocalContext.current
    val peDataConfigured = remember { mutableStateOf(false) }

    // if PE data is already configured do nothing
    if (peDataConfigured.value) return peDataConfigured

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            PeData.configure(context).onFailure {
                onFailed(it)
            }.onSuccess {
                peDataConfigured.value = true
            }
        }
    }
    return peDataConfigured
}
