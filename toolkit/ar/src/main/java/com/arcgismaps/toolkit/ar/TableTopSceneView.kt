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
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DeviceOrientation
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
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Pose
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume

/**
 * Displays a [SceneView] in a tabletop AR environment.
 *
 * @param arcGISScene the [ArcGISScene] to be rendered by this TableTopSceneView
 * @param arcGISSceneAnchor the [Point] in the [ArcGISScene] where the AR scene will initially be centered
 * @param translationFactor the factor defines how much the scene view translates as the device moves.
 * @param modifier Modifier to be applied to the TableTopSceneView
 * @param clippingDistance the clipping distance in meters around the camera. A null means that no data will be clipped.
 * @param onInitializationStatusChanged a callback that is invoked when the initialization status of the [TableTopSceneView] changes.
 * @param requestCameraPermissionAutomatically whether to request the camera permission automatically.
 * If set to `true`, the camera permission will be requested automatically when the composable is
 * first displayed. The default value is `true`.
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale]
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry]
 * @param graphicsOverlays graphics overlays used by this TableTopSceneView
 * @param tableTopSceneViewProxy the [TableTopSceneViewProxy] to associate with the TableTopSceneView
 * @param sceneViewInteractionOptions the [SceneViewInteractionOptions] used by this TableTopSceneView
 * @param viewLabelProperties the [ViewLabelProperties] used by the TableTopSceneView
 * @param selectionProperties the [SelectionProperties] used by the TableTopSceneView
 * @param isAttributionBarVisible true if attribution bar is visible in the TableTopSceneView, false otherwise
 * @param onAttributionTextChanged lambda invoked when the attribution text of the TableTopSceneView has changed
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes
 * @param analysisOverlays analysis overlays that render the results of 3D visual analysis on the TableTopSceneView
 * @param imageOverlays image overlays for displaying images in the TableTopSceneView
 * @param timeExtent the [TimeExtent] used by the TableTopSceneView
 * @param onTimeExtentChanged lambda invoked when the TableTopSceneView's [TimeExtent] is changed
 * @param sunTime the position of the sun in the TableTopSceneView based on a specific date and time
 * @param sunLighting the type of ambient sunlight and shadows in the TableTopSceneView
 * @param ambientLightColor the color of the TableTopSceneView's ambient light
 * @param onNavigationChanged lambda invoked when the navigation status of the TableTopSceneView has changed
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the TableTopSceneView has changed
 * @param onLayerViewStateChanged lambda invoked when the TableTopSceneView's layer view state is changed
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the TableTopSceneView
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the TableTopSceneView has changed
 * @param onRotate lambda invoked when a user performs a rotation gesture on the TableTopSceneView
 * @param onScale lambda invoked when a user performs a pinch gesture on the TableTopSceneView
 * @param onUp lambda invoked when the user removes all their pointers from the TableTopSceneView
 * @param onDown lambda invoked when the user first presses on the TableTopSceneView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the TableTopSceneView
 * @param onDoubleTap lambda invoked the user double taps on the TableTopSceneView
 * @param onLongPress lambda invoked when a user holds a pointer on the TableTopSceneView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the TableTopSceneView
 * @param onPan lambda invoked when a user drags a pointer or pointers across TableTopSceneView
 * @param onDrawStatusChanged lambda invoked when the draw status of the TableTopSceneView is changed
 * @param content the content of the TableTopSceneView
 *
 * @since 200.6.0
 */
@Composable
public fun TableTopSceneView(
    arcGISScene: ArcGISScene,
    arcGISSceneAnchor: Point,
    translationFactor: Double,
    modifier: Modifier = Modifier,
    clippingDistance: Double? = null,
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
    val initializationStatus = rememberTableTopSceneViewStatus()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermissionGranted by rememberCameraPermission(requestCameraPermissionAutomatically) {
        // onNotGranted
        initializationStatus.update(
            TableTopSceneViewStatus.FailedToInitialize(
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
                TableTopSceneViewStatus.FailedToInitialize(
                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
                ),
                onInitializationStatusChanged
            )
        } else {
            arCoreInstalled = true
        }
    }

    val cameraController = remember {
        TransformationMatrixCameraController().apply {
            setOriginCamera(Camera(arcGISSceneAnchor, heading = 0.0, pitch = 90.0, roll = 0.0))
            setTranslationFactor(translationFactor)
            this.clippingDistance = clippingDistance ?: 0.0
        }
    }
    var arCoreAnchor: Anchor? by remember { mutableStateOf(null) }
    var visualizePlanes by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        if (cameraPermissionGranted && arCoreInstalled) {
            val arSessionWrapper =
                rememberArSessionWrapper(applicationContext = context.applicationContext)
            DisposableEffect(Unit) {
                // We call this from inside DisposableEffect so that we invoke the callback in a side effect
                initializationStatus.update(
                    TableTopSceneViewStatus.DetectingPlanes,
                    onInitializationStatusChanged
                )
                lifecycleOwner.lifecycle.addObserver(arSessionWrapper)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(arSessionWrapper)
                    arSessionWrapper.onDestroy(lifecycleOwner)
                }
            }
            val identityMatrix = remember { TransformationMatrix.createIdentityMatrix() }
            ArCameraFeed(
                arSessionWrapper = arSessionWrapper,
                onFrame = { frame, displayRotation ->
                    arCoreAnchor?.let { anchor ->
                        val anchorPosition = identityMatrix - anchor.pose.translation.let {
                            TransformationMatrix.createWithQuaternionAndTranslation(
                                0.0,
                                0.0,
                                0.0,
                                1.0,
                                it[0].toDouble(),
                                it[1].toDouble(),
                                it[2].toDouble()
                            )
                        }
                        val cameraPosition =
                            anchorPosition + frame.camera.displayOrientedPose.transformationMatrix
                        cameraController.transformationMatrix = cameraPosition
                        val imageIntrinsics = frame.camera.imageIntrinsics
                        tableTopSceneViewProxy.sceneViewProxy.setFieldOfViewFromLensIntrinsics(
                            imageIntrinsics.focalLength[0],
                            imageIntrinsics.focalLength[1],
                            imageIntrinsics.principalPoint[0],
                            imageIntrinsics.principalPoint[1],
                            imageIntrinsics.imageDimensions[0].toFloat(),
                            imageIntrinsics.imageDimensions[1].toFloat(),
                            deviceOrientation = when (displayRotation) {
                                0 -> DeviceOrientation.Portrait
                                90 -> DeviceOrientation.LandscapeRight
                                180 -> DeviceOrientation.ReversePortrait
                                270 -> DeviceOrientation.LandscapeLeft
                                else -> DeviceOrientation.Portrait
                            }
                        )
                        tableTopSceneViewProxy.sceneViewProxy.renderFrame()
                    }
                },
                onTapWithHitResult = { hit ->
                    hit?.let { hitResult ->
                        if (arCoreAnchor == null) {
                            arCoreAnchor = hitResult.createAnchor()
                            // stop rendering planes
                            visualizePlanes = false
                        }
                    }
                },
                onFirstPlaneDetected = {
                    initializationStatus.update(
                        TableTopSceneViewStatus.Initialized,
                        onInitializationStatusChanged
                    )
                },
                visualizePlanes = visualizePlanes
            )
        }
        if (initializationStatus.value == TableTopSceneViewStatus.Initialized && arCoreAnchor != null) {
            SceneView(
                arcGISScene = arcGISScene,
                modifier = Modifier.fillMaxSize(),
                onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
                onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
                graphicsOverlays = graphicsOverlays,
                sceneViewProxy = tableTopSceneViewProxy.sceneViewProxy,
                sceneViewInteractionOptions = sceneViewInteractionOptions,
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
                onDrawStatusChanged = onDrawStatusChanged,
                content = {
                    content?.invoke(TableTopSceneViewScope(this))
                }
            )
        }
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

private suspend fun checkArCoreAvailability(context: Context): ArCoreApk.Availability =
    suspendCancellableCoroutine { continuation ->
        ArCoreApk.getInstance().checkAvailabilityAsync(context) {
            continuation.resume(it)
        }
    }

private fun MutableState<TableTopSceneViewStatus>.update(
    newStatus: TableTopSceneViewStatus,
    callback: ((TableTopSceneViewStatus) -> Unit)?
) {
    this.value = newStatus
    callback?.invoke(newStatus)
}

/**
 * Returns a [TransformationMatrix] based on the [Pose]'s rotation and translation.
 *
 * @since 200.6.0
 */
private val Pose.transformationMatrix: TransformationMatrix
    get() {
        return TransformationMatrix.createWithQuaternionAndTranslation(
            rotationQuaternion[0].toDouble(),
            rotationQuaternion[1].toDouble(),
            rotationQuaternion[2].toDouble(),
            rotationQuaternion[3].toDouble(),
            translation[0].toDouble(),
            translation[1].toDouble(),
            translation[2].toDouble()
        )
    }
