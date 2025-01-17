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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDataSource
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.SystemLocationDataSource
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
import com.arcgismaps.toolkit.ar.internal.LocationDataSourceWrapper
import com.arcgismaps.toolkit.ar.internal.PermissionState
import com.arcgismaps.toolkit.ar.internal.checkArCoreAvailability
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberCameraPermission
import com.arcgismaps.toolkit.ar.internal.setFieldOfViewFromLensIntrinsics
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
public fun WorldScaleSceneView(
    arcGISScene: ArcGISScene,
    modifier: Modifier = Modifier,
    onInitializationStatusChanged: ((WorldScaleSceneViewStatus) -> Unit)? = null,
    requestCameraPermissionAutomatically: Boolean = true,
    requestLocationPermissionAutomatically: Boolean = true,
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val permissionState = remember { PermissionState(context, Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION) }
    val isPermissionsGranted by permissionState.isGrantedState.collectAsStateWithLifecycle()
    val hasLaunchedRequest by permissionState.hasLaunchedRequest.collectAsStateWithLifecycle()
    if (!isPermissionsGranted && !hasLaunchedRequest) {
        permissionState.LaunchRequest()
    }
    LaunchedEffect(hasLaunchedRequest, isPermissionsGranted) {
        if (hasLaunchedRequest && !isPermissionsGranted) {
            val permissionsGranted = permissionState.getPermissionsGranted()
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(
                    IllegalStateException(
                        context.getString(
                            R.string.permissions_not_granted_message,
                            permissionsGranted.filter { !it.value }.keys.joinToString(", ")
                        )
                    )
                ),
                onInitializationStatusChanged
            )
        }
    }

    var arCoreInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val arCoreAvailability = checkArCoreAvailability(context)
        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
                ),
                onInitializationStatusChanged
            )
        } else {
            arCoreInstalled = true
        }
    }

    val cameraController = remember { TransformationMatrixCameraController() }

    var isLocationDataSourceStarted by remember { mutableStateOf(false) }
        if (isPermissionsGranted) {
            val locationDataSource = rememberSystemLocationDataSource()
            TrackLocationWithLocationDataSource(
                locationDataSource,
                cameraController,
                onLocationDataSourceStatus = {
                    when (it) {
                        LocationDataSourceStatus.FailedToStart -> {
                            initializationStatus.update(
                                WorldScaleSceneViewStatus.FailedToInitialize(
                                    IllegalStateException(
                                        context.getString(
                                            R.string.location_data_source_failed_to_start,
                                            locationDataSource.error.value?.message
                                        )
                                    )
                                ),
                                onInitializationStatusChanged
                            )
                        }

                        LocationDataSourceStatus.Started -> {
                            isLocationDataSourceStarted = true
                        }

                        else -> {}
                    }
                }
            )
    }

    Box(modifier = modifier) {
        if (isPermissionsGranted && isLocationDataSourceStarted && arCoreInstalled) {
            val arSessionWrapper =
                rememberArSessionWrapper(applicationContext = context.applicationContext)
            DisposableEffect(Unit) {
                lifecycleOwner.lifecycle.addObserver(arSessionWrapper)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(arSessionWrapper)
                    arSessionWrapper.onDestroy(lifecycleOwner)
                }
            }
            val session = arSessionWrapper.session.collectAsStateWithLifecycle()
            session.value?.let { arSession ->
                ArCameraFeed(
                    session = arSession,
                    onFrame = { frame, displayRotation ->
                        val cameraPosition = frame.camera.displayOrientedPose.transformationMatrix
                        cameraController.transformationMatrix = cameraPosition
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
            }
        }
        if (initializationStatus.value == WorldScaleSceneViewStatus.Initialized) {
            // Disable interaction, which is not supported in WorldScale scenarios
            val interactionOptions = remember {
                SceneViewInteractionOptions().apply {
                    this.isEnabled = false
                }
            }
            SceneView(
                arcGISScene = arcGISScene,
                modifier = Modifier.fillMaxSize(),
                onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
                onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
                graphicsOverlays = graphicsOverlays,
                sceneViewProxy = worldScaleSceneViewProxy.sceneViewProxy,
                sceneViewInteractionOptions = interactionOptions,
                viewLabelProperties = viewLabelProperties,
                selectionProperties = selectionProperties,
                isAttributionBarVisible = isAttributionBarVisible,
                onAttributionTextChanged = onAttributionTextChanged,
                onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
                cameraController = cameraController,
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
                    content?.invoke(WorldScaleSceneViewScope(this))
                }
            )
        }
    }
}

@Composable
private fun TrackLocationWithLocationDataSource(
    locationDataSource: LocationDataSource,
    cameraController: TransformationMatrixCameraController,
    onLocationDataSourceStatus: ((LocationDataSourceStatus)) -> Unit
) {
    var hasSetOriginCamera by remember { mutableStateOf(false) }
    var initialHeading: Double? by remember { mutableStateOf(null) }
    LaunchedEffect(locationDataSource) {
        launch {
            locationDataSource.status.collect {
                onLocationDataSourceStatus(it)
            }
        }
        launch {
            locationDataSource.locationChanged.collect { location ->
                if (!hasSetOriginCamera) {
                    cameraController.setOriginCamera(
                        Camera(
                            location.position.y,
                            location.position.x,
                            if (location.position.hasZ) location.position.z!! else 1.0,
                            0.0,
                            90.0,
                            0.0
                        )
                    )
                    hasSetOriginCamera = true
                }
            }
        }
        launch {
            locationDataSource.headingChanged.take(1).collect { heading ->
                if (!heading.isNaN()) {
                    if (initialHeading == null) {
                        cameraController.setOriginCamera(
                            Camera(
                                cameraController.originCamera.value.location,
                                heading,
                                cameraController.originCamera.value.pitch,
                                cameraController.originCamera.value.roll
                            )
                        )
                        initialHeading = heading
                    }
                }
            }
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(locationDataSource) {
        val wrapper = LocationDataSourceWrapper(locationDataSource)
        lifecycleOwner.lifecycle.addObserver(wrapper)
        wrapper.startLocationDataSource()
        onDispose {
            wrapper.onDestroy(lifecycleOwner)
            lifecycleOwner.lifecycle.removeObserver(wrapper)
        }
    }
}

@Composable
private fun rememberSystemLocationDataSource(): SystemLocationDataSource {
    ArcGISEnvironment.applicationContext = LocalContext.current.applicationContext
    return remember { SystemLocationDataSource() }
}

/**
 * Checks if the location permission is granted and requests it if required.
 *
 * @since 200.7.0
 */
@Composable
internal fun rememberLocationPermission(
    requestLocationPermissionAutomatically: Boolean,
    onNotGranted: () -> Unit
): MutableState<Boolean> {
    val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
    val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val context = LocalContext.current
    // We need to track whether we've already requested, otherwise, when the permission is denied,
    // the permission request dialog will be shown again on the next composition
    var hasRequested by remember { mutableStateOf(false) }
    val isGrantedState = remember {
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            coarseLocationPermission
        ) == PackageManager.PERMISSION_GRANTED
        val fineLocationPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            fineLocationPermission
        ) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(coarseLocationGranted && fineLocationPermissionGranted)
    }
    if (!isGrantedState.value && !hasRequested) {
        if (requestLocationPermissionAutomatically) {
            val requestPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { granted ->
                    isGrantedState.value = granted.all { it.value }
                    if (!isGrantedState.value) {
                        onNotGranted()
                    }
                }
            SideEffect {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
                hasRequested = true
            }
        } else {
            // We should use a SideEffect here to ensure that code executed in onNotGranted is run
            // after the composition completes, for example, invoking the onInitializationStatusChanged
            // callback
            SideEffect {
                onNotGranted()
            }
        }
    }
    return isGrantedState
}
