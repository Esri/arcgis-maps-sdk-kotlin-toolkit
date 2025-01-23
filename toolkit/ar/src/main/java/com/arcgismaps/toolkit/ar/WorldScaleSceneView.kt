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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.Location
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
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.LocationDataSourceWrapper
import com.arcgismaps.toolkit.ar.internal.checkArCoreAvailability
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.setFieldOfViewFromLensIntrinsics
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.flow.filter
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

    val hasCompletedPermissionRequest = requestPermissionsOrFail(
        context,
        requestCameraPermissionAutomatically,
        requestLocationPermissionAutomatically,
        initializationStatus,
        onInitializationStatusChanged,
    )

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
    if (hasCompletedPermissionRequest && context.checkPermissionsGranted()) {
        val locationDataSource = rememberSystemLocationDataSource()
        LocationTracker(
            locationDataSource,
            cameraController,
            onLocationDataSourceStatus = { status ->
                when (status) {
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
        if (isLocationDataSourceStarted && arCoreInstalled) {
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

/**
 * Checks the permissions required for the [WorldScaleSceneView] to function and requests them if necessary.
 * If [requestCameraPermissionAutomatically] or [requestLocationPermissionAutomatically] are set to false,
 * the permissions will not be requested automatically and the initialization will fail if the permissions are not granted.
 *
 * @return A [MutableState] that will be true when the permissions have been requested and the result has been received.
 * @since 200.7.0
 */
@Composable
private fun requestPermissionsOrFail(
    context: Context,
    requestCameraPermissionAutomatically: Boolean,
    requestLocationPermissionAutomatically: Boolean,
    initializationStatus: MutableState<WorldScaleSceneViewStatus>,
    onInitializationStatusChanged: ((WorldScaleSceneViewStatus) -> Unit)?
): Boolean {
    var hasLaunchedRequest by remember { mutableStateOf(false) }
    var hasCompletedRequest by remember { mutableStateOf(false) }
    val permissionsToRequest = mutableListOf<String>()
    if (requestCameraPermissionAutomatically) {
        permissionsToRequest.add(Manifest.permission.CAMERA)
    } else {
        // If we are not requesting the camera permission automatically, we should check if it's granted
        // and fail early if not
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.camera_permission_not_granted))
                ),
                onInitializationStatusChanged
            )
        }
    }
    if (requestLocationPermissionAutomatically) {
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        // If we are not requesting the location permissions automatically, we should check if it's granted
        // and fail early if not
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            initializationStatus.update(
                WorldScaleSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.location_permission_not_granted))
                ),
                onInitializationStatusChanged
            )
        }
    }
    val launcher =
        rememberLauncherForActivityResult<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grantedState: Map<String, Boolean> ->
            // This callback will only be relevant to the permissions that were added to the request.
            // If the user has set [requestCameraPermissionAutomatically] to false, for example,
            // and the camera permission is not granted, this will not be included in the message.
            if (grantedState.any { !it.value }) {
                val permissionsNotGranted =
                    grantedState.filter { !it.value }.keys.joinToString(", ")
                initializationStatus.update(
                    WorldScaleSceneViewStatus.FailedToInitialize(
                        IllegalStateException(
                            context.getString(
                                R.string.permissions_not_granted_message,
                                permissionsNotGranted
                            )
                        )
                    ),
                    onInitializationStatusChanged
                )
            }
            hasCompletedRequest = true
        }

    if (!hasLaunchedRequest) {
        SideEffect {
            hasLaunchedRequest = true
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    return hasCompletedRequest
}

/**
 * Returns true if camera and location permissions are granted, otherwise false if either is not granted.
 *
 * @since 200.7.0
 */
private fun Context.checkPermissionsGranted(): Boolean = listOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
).map { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}.all { it }

/**
 * Starts the [locationDataSource] and periodically updates the [cameraController] with new locations
 *
 * @since 200.7.0
 */
@Composable
private fun LocationTracker(
    locationDataSource: LocationDataSource,
    cameraController: TransformationMatrixCameraController,
    onLocationDataSourceStatus: ((LocationDataSourceStatus)) -> Unit
) {
    var hasSetOriginCamera by remember { mutableStateOf(false) }
    LaunchedEffect(locationDataSource) {
        launch {
            locationDataSource.status.collect {
                onLocationDataSourceStatus(it)
            }
        }
        launch {
            locationDataSource.locationChanged
                .filter { location ->
                    !hasSetOriginCamera || shouldUpdateCamera(
                        location,
                        cameraController.originCamera.value
                    )
                }
                .collect { location ->
                    cameraController.setOriginCamera(
                        Camera(
                            location.position.y,
                            location.position.x,
                            if (location.position.hasZ) location.position.z!! else 0.0,
                            0.0,
                            90.0,
                            0.0
                        )
                    )
                    if (!hasSetOriginCamera) {
                        hasSetOriginCamera = true
                    }

                    // We have to do this or the error gets bigger and bigger.
                    cameraController.transformationMatrix =
                        TransformationMatrix.createIdentityMatrix()
                }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
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

/**
 * Returns false if the location timestamp is older than 10 seconds,
 * if the horizontal or vertical accuracy is negative,
 * or if the distance between the location and the current camera is less than 2 meters.
 * Otherwise, returns true.
 *
 * @since 200.7.0
 */
internal fun shouldUpdateCamera(
    location: Location,
    currentCamera: Camera,
): Boolean {
    // filter out old locations
    if (Instant.now()
            .toEpochMilli() - location.timestamp.toEpochMilli() > WorldScaleParameters.LOCATION_AGE_THRESHOLD_MS
    ) return false

    // filter out locations with no accuracy
    if (location.horizontalAccuracy < 0.0
        || location.verticalAccuracy < 0.0
        || location.horizontalAccuracy.isNaN()
        || location.verticalAccuracy.isNaN()
    ) return false

    val distance = GeometryEngine.distanceGeodeticOrNull(
        currentCamera.location,
        location.position,
        distanceUnit = LinearUnit.meters,
        azimuthUnit = null,
        curveType = GeodeticCurveType.Geodesic
    )?.distance ?: return false
    return distance > WorldScaleParameters.LOCATION_DISTANCE_THRESHOLD_METERS
}

@Composable
private fun rememberSystemLocationDataSource(): SystemLocationDataSource {
    ArcGISEnvironment.applicationContext = LocalContext.current.applicationContext
    return remember { SystemLocationDataSource() }
}

/**
 * Provides constants for the [WorldScaleSceneView].
 *
 * @since 200.7.0
 */
private data object WorldScaleParameters {
    const val LOCATION_DISTANCE_THRESHOLD_METERS = 2.0
    const val LOCATION_AGE_THRESHOLD_MS = 10000.0
}
