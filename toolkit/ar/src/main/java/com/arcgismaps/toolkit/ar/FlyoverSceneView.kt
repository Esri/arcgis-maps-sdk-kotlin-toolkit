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
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.geometry.Point
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
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberPermissionsGranted
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import java.time.Instant

@Composable
public fun FlyoverSceneView(
    arcGISScene: ArcGISScene,
    initialLocation: Point,
    initialHeading: Double,
    translationFactor: Double,
    modifier: Modifier = Modifier,
    flyoverSceneViewProxy: FlyoverSceneViewProxy = remember { FlyoverSceneViewProxy() },
    interactionOptions: SceneViewInteractionOptions = remember { SceneViewInteractionOptions() },
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
    val context = LocalContext.current

    val cameraPermissionGranted by rememberPermissionsGranted(listOf(Manifest.permission.CAMERA)) {
        // onNotGranted
//        initializationStatus.update(
//            TFlyoverSceneViewStatus.FailedToInitialize(
//                IllegalStateException(
//                    context.getString(R.string.camera_permission_not_granted)
//                )
//            ),
//            onInitializationStatusChanged
//        )
    }

    var arCoreInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
//        val arCoreAvailability = checkArCoreAvailability(context)
//        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
//            initializationStatus.update(
//                FlyoverSceneViewStatus.FailedToInitialize(
//                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
//                ),
//                onInitializationStatusChanged
//            )
//        } else {
        arCoreInstalled = true
//        }
    }

    if (!cameraPermissionGranted) {
        return
    }

    val arSessionWrapper =
        rememberArSessionWrapper(
            applicationContext = context.applicationContext,
            planeFindingMode = Config.PlaneFindingMode.DISABLED
        )

    val cameraController = remember {
        TransformationMatrixCameraController()
    }

    LaunchedEffect(initialLocation, initialHeading) {
        Log.d("Blah", "Launched effect")
        cameraController.setOriginCamera(
            Camera(
                locationPoint = initialLocation,
                pitch = 90.0,
                roll = 0.0,
                heading = initialHeading
            )
        )
        arSessionWrapper.resetSession(planeFindingMode = Config.PlaneFindingMode.DISABLED)
    }

    LaunchedEffect(translationFactor) {
        cameraController.setTranslationFactor(translationFactor)
    }

    Box(modifier = Modifier) {
        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation, session ->
                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    cameraController.transformationMatrix =
                        frame.camera.displayOrientedPose.transformationMatrix
                }
                flyoverSceneViewProxy.sceneViewProxy.renderFrame()
            },
            onTapWithHitResult = {},
            onFirstPlaneDetected = {},
            visualizePlanes = false
        )
    }

    SceneView(
        arcGISScene = arcGISScene,
        sceneViewProxy = flyoverSceneViewProxy.sceneViewProxy,
        cameraController = cameraController,
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
