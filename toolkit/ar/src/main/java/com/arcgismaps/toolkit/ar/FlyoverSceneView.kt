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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.arcgismaps.toolkit.ar.internal.checkArCoreAvailability
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberPermissionsGranted
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import java.time.Instant

/**
 * A scene view that provides an augmented reality fly over experience.
 *
 * @param arcGISScene the [ArcGISScene] to be rendered by this FlyoverSceneView.
 * @param modifier Modifier to be applied to the FlyoverSceneView.
 * @param flyoverSceneViewProxy the [FlyoverSceneViewProxy] to associate with the FlyoverSceneView.
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale].
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry].
 * @param graphicsOverlays graphics overlays used by this FlyoverSceneView.
 * @param flyoverSceneViewProxy the [FlyoverSceneViewProxy] to associate with the FlyoverSceneView.
 * @param viewLabelProperties the [ViewLabelProperties] used by the FlyoverSceneView.
 * @param selectionProperties the [SelectionProperties] used by the FlyoverSceneView.
 * @param isAttributionBarVisible true if attribution bar is visible in the FlyoverSceneView, false otherwise.
 * @param onAttributionTextChanged lambda invoked when the attribution text of the FlyoverSceneView has changed.
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes.
 * @param analysisOverlays analysis overlays that render the results of 3D visual analysis on the FlyoverSceneView.
 * @param imageOverlays image overlays for displaying images in the FlyoverSceneView.
 * @param timeExtent the [TimeExtent] used by the FlyoverSceneView.
 * @param onTimeExtentChanged lambda invoked when the FlyoverSceneView's [TimeExtent] is changed.
 * @param sunTime the position of the sun in the FlyoverSceneView based on a specific date and time.
 * @param sunLighting the type of ambient sunlight and shadows in the FlyoverSceneView.
 * @param ambientLightColor the color of the FlyoverSceneView's ambient light.
 * @param onNavigationChanged lambda invoked when the navigation status of the FlyoverSceneView has changed.
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the FlyoverSceneView has changed.
 * @param onLayerViewStateChanged lambda invoked when the FlyoverSceneView's layer view state is changed.
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the FlyoverSceneView.
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the FlyoverSceneView has changed.
 * @param onRotate lambda invoked when a user performs a rotation gesture on the FlyoverSceneView.
 * @param onScale lambda invoked when a user performs a pinch gesture on the FlyoverSceneView.
 * @param onUp lambda invoked when the user removes all their pointers from the FlyoverSceneView.
 * @param onDown lambda invoked when the user first presses on the FlyoverSceneView.
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the FlyoverSceneView.
 * @param onDoubleTap lambda invoked the user double taps on the FlyoverSceneView.
 * @param onLongPress lambda invoked when a user holds a pointer on the FlyoverSceneView.
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the FlyoverSceneView.
 * @param onPan lambda invoked when a user drags a pointer or pointers across FlyoverSceneView.
 *
 * @since 200.8.0
 */
@Composable
public fun FlyoverSceneView(
    arcGISScene: ArcGISScene,
    flyoverSceneViewProxy: FlyoverSceneViewProxy,
    modifier: Modifier = Modifier,
    interactionOptions: SceneViewInteractionOptions = remember { SceneViewInteractionOptions() },
    onInitializationStatusChanged: ((FlyoverSceneViewStatus) -> Unit)? = null,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    viewLabelProperties: ViewLabelProperties = remember { ViewLabelProperties() },
    selectionProperties: SelectionProperties = remember { SelectionProperties() },
    isAttributionBarVisible: Boolean = true,
    onAttributionTextChanged: ((String) -> Unit)? = null,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null,
    analysisOverlays: List<AnalysisOverlay> = remember { emptyList() },
    imageOverlays: List<ImageOverlay> = remember { emptyList() },
    atmosphereEffect: AtmosphereEffect = AtmosphereEffect.Realistic,
    timeExtent: TimeExtent? = null,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)? = null,
    spaceEffect: SpaceEffect = SpaceEffect.Stars,
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
    onPan: ((PanChangeEvent) -> Unit)? = null
) {
    val initializationStatus = rememberFlyoverSceneViewStatus()

    val context = LocalContext.current

    val cameraPermissionGranted by rememberPermissionsGranted(listOf(Manifest.permission.CAMERA)) {
        // onNotGranted
        initializationStatus.update(
            FlyoverSceneViewStatus.FailedToInitialize(
                IllegalStateException(
                    context.getString(R.string.camera_permission_not_granted)
                )
            ),
            onInitializationStatusChanged
        )
    }

    var arCoreInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val arCoreAvailability = checkArCoreAvailability(context)
        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            initializationStatus.update(
                FlyoverSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
                ),
                onInitializationStatusChanged
            )
        } else {
            arCoreInstalled = true
        }
    }

    if (!cameraPermissionGranted) {
        return
    }

    // if we get here camera permission is already granted so if ArCore is installed we are initialized
    if (arCoreInstalled) {
        initializationStatus.update(
            FlyoverSceneViewStatus.Initialized,
            onInitializationStatusChanged
        )
    }

    val arSessionWrapper =
        rememberArSessionWrapper(
            applicationContext = context.applicationContext,
            planeFindingMode = Config.PlaneFindingMode.DISABLED
        )

    DisposableEffect(flyoverSceneViewProxy) {
        flyoverSceneViewProxy.setSessionWrapper(arSessionWrapper)
        onDispose {
            flyoverSceneViewProxy.setSessionWrapper(null)
        }
    }

    Box(modifier = Modifier) {
        // use rememberUpdatedState so that the lambda used below always sees the latest proxy
        val proxy by rememberUpdatedState(flyoverSceneViewProxy)

        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation, session ->
                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    proxy.cameraController.transformationMatrix =
                        frame.camera.displayOrientedPose.transformationMatrix
                }
                proxy.sceneViewProxy.renderFrame()
            },
            onTapWithHitResult = {},
            onFirstPlaneDetected = {},
            visualizePlanes = false
        )
    }

    SceneView(
        arcGISScene = arcGISScene,
        sceneViewProxy = flyoverSceneViewProxy.sceneViewProxy,
        cameraController = flyoverSceneViewProxy.cameraController,
        atmosphereEffect = atmosphereEffect,
        spaceEffect = spaceEffect,
        modifier = modifier,
        onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
        graphicsOverlays = graphicsOverlays,
        sceneViewInteractionOptions = interactionOptions,
        viewLabelProperties = viewLabelProperties,
        selectionProperties = selectionProperties,
        isAttributionBarVisible = isAttributionBarVisible,
        onAttributionTextChanged = onAttributionTextChanged,
        onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
        analysisOverlays = analysisOverlays,
        imageOverlays = imageOverlays,
        timeExtent = timeExtent,
        onTimeExtentChanged = onTimeExtentChanged,
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
        onPan = onPan
    )
}
