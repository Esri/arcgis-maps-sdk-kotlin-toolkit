/*
 *
 *  Copyright 2024 Esri
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
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
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
import com.arcgismaps.mapping.view.DrawStatus
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
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume

/**
 * Displays a [SceneView] in a tabletop AR environment.
 *
 * @param requestCameraPermissionAutomatically whether to request the camera permission automatically.
 * If set to `true`, the camera permission will be requested automatically when the composable is
 * first displayed. The default value is `true`.
 * @since 200.6.0
 */
@Composable
fun TableTopSceneView(
    arcGISScene: ArcGISScene,
    modifier: Modifier = Modifier,
    onInitializationStatusChanged: ((TableTopSceneViewStatus) -> Unit)? = null,
    requestCameraPermissionAutomatically: Boolean = true,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
    tableTopSceneViewProxy: TableTopSceneViewProxy = remember { TableTopSceneViewProxy() },
    sceneViewInteractionOptions: SceneViewInteractionOptions = remember { SceneViewInteractionOptions() },
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
    onDrawStatusChanged: ((DrawStatus) -> Unit)? = null,
    content: (@Composable TableTopSceneViewScope.() -> Unit)? = null
) {
    var initializationStatus: TableTopSceneViewStatus by remember {
        mutableStateOf(
            TableTopSceneViewStatus.Initializing
        )
    }
    val updateStatus = remember {
        { newStatus: TableTopSceneViewStatus ->
            initializationStatus = newStatus
            onInitializationStatusChanged?.invoke(newStatus)
        }
    }
    updateStatus(TableTopSceneViewStatus.Initializing)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermissionGranted by rememberCameraPermission(requestCameraPermissionAutomatically) {
        // onNotGranted
        updateStatus(
            TableTopSceneViewStatus.FailedToInitialize(
                IllegalStateException(
                    context.getString(R.string.camera_permission_not_granted)
                )
            )
        )
    }

    var arCoreInstalled by remember { mutableStateOf(false) }

    // invoked when the cameraPermissionGranted state changes to true
    LaunchedEffect(Unit) {
        val arCoreAvailability = checkArCoreAvailability(context)
        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            updateStatus(
                TableTopSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
                )
            )
        } else {
            arCoreInstalled = true
        }
    }

    Box(modifier = modifier) {
        if (cameraPermissionGranted && arCoreInstalled) {
            // invoked when the arCoreInstalled state changes to true
            val arSessionWrapper =
                rememberArSessionWrapper(applicationContext = context.applicationContext)
            updateStatus(TableTopSceneViewStatus.Initialized)
            DisposableEffect(Unit) {
                lifecycleOwner.lifecycle.addObserver(arSessionWrapper)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(arSessionWrapper)
                    arSessionWrapper.onDestroy(lifecycleOwner)
                }
            }
            ArCameraFeed(arSessionWrapper = arSessionWrapper, onFrame = {}, onTap = {})
        }
        SceneView(
            arcGISScene = arcGISScene,
            modifier = Modifier.fillMaxSize(),
            onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
            onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
            graphicsOverlays =
            graphicsOverlays,
            sceneViewProxy = tableTopSceneViewProxy.sceneViewProxy,
            sceneViewInteractionOptions = sceneViewInteractionOptions,
            viewLabelProperties = viewLabelProperties,
            selectionProperties = selectionProperties,
            isAttributionBarVisible = isAttributionBarVisible,
            onAttributionTextChanged = onAttributionTextChanged,
            onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
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
            onSpatialReferenceChanged = {
                onSpatialReferenceChanged?.invoke(it)
            },
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
            onDrawStatusChanged = onDrawStatusChanged,
            content = {
                content?.invoke(TableTopSceneViewScope(this))
            }
        )
    }
}

/**
 * Checks if the camera permission is granted and requests it if required.
 *
 * @since 200.6.0
 */
@Composable
private fun rememberCameraPermission(
    requestCameraPermissionAutomatically: Boolean,
    onNotGranted: () -> Unit
): MutableState<Boolean> {
    val cameraPermission = Manifest.permission.CAMERA
    val context = LocalContext.current
    val isGrantedState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    if (!isGrantedState.value) {
        if (requestCameraPermissionAutomatically) {
            val requestPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
                    isGrantedState.value = granted
                    if (!granted) {
                        onNotGranted()
                    }
                }
            SideEffect {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            onNotGranted()
        }
    }
    return isGrantedState
}

private suspend fun checkArCoreAvailability(context: Context): ArCoreApk.Availability =
    suspendCancellableCoroutine { continuation ->
        ArCoreApk.getInstance().checkAvailabilityAsync(context) {
            continuation.resume(it)
        }
    }
