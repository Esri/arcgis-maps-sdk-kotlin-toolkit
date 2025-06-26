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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.rememberArCoreInstalled
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberCameraPermission
import com.arcgismaps.toolkit.ar.internal.setFieldOfViewFromLensIntrinsics
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.ar.internal.update
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import java.time.Instant

/**
 * A scene view that provides an augmented reality table top experience.
 *
 * Note: You must follow [Google's user privacy requirements for ARCore](https://developers.google.com/ar/develop/privacy-requirements)
 * when using TableTopSceneView in your application.
 *
 * @param arcGISScene the [ArcGISScene] to be rendered by this TableTopSceneView.
 * @param arcGISSceneAnchor the [Point] in the [ArcGISScene] used to anchor the scene with a physical surface.
 * @param translationFactor determines how many meters the scene view translates as the device moves.
 * A useful formula for determining this value is `translation factor = virtual content width / desired physical content width`.
 * The virtual content width is the real-world size of the scene content and the desired physical content width is the physical
 * table top width. The virtual content width is determined by the [clippingDistance] in meters around the [arcGISSceneAnchor].
 * For example, in order to setup a table top scene where scene data should be displayed within a 400 meter radius around
 * the [arcGISSceneAnchor] and be placed on a table top that is 1 meter wide: `translation factor = 400 meter / 1 meter`.
 * @param modifier Modifier to be applied to the TableTopSceneView.
 * @param clippingDistance the clipping distance in meters around the [arcGISSceneAnchor]. A null value means that no data will be clipped.
 * @param onInitializationStatusChanged a callback that is invoked when the initialization status of the [TableTopSceneView] changes.
 * @param requestCameraPermissionAutomatically whether to request the camera permission automatically.
 * If set to `true`, the camera permission will be requested automatically when the composable is
 * first displayed. The default value is `true`. Set to false if your application takes care of requesting camera permissions before
 * displaying the TableTopSceneView.
 * @param onViewpointChangedForCenterAndScale lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.CenterAndScale].
 * @param onViewpointChangedForBoundingGeometry lambda invoked when the viewpoint changes, passing a viewpoint
 * type of [ViewpointType.BoundingGeometry].
 * @param graphicsOverlays graphics overlays used by this TableTopSceneView.
 * @param tableTopSceneViewProxy the [TableTopSceneViewProxy] to associate with the TableTopSceneView.
 * @param viewLabelProperties the [ViewLabelProperties] used by the TableTopSceneView.
 * @param selectionProperties the [SelectionProperties] used by the TableTopSceneView.
 * @param isAttributionBarVisible true if attribution bar is visible in the TableTopSceneView, false otherwise.
 * @param onAttributionTextChanged lambda invoked when the attribution text of the TableTopSceneView has changed.
 * @param onAttributionBarLayoutChanged lambda invoked when the attribution bar's position or size changes.
 * @param analysisOverlays analysis overlays that render the results of 3D visual analysis on the TableTopSceneView.
 * @param imageOverlays image overlays for displaying images in the TableTopSceneView.
 * @param timeExtent the [TimeExtent] used by the TableTopSceneView.
 * @param onTimeExtentChanged lambda invoked when the TableTopSceneView's [TimeExtent] is changed.
 * @param sunTime the position of the sun in the TableTopSceneView based on a specific date and time.
 * @param sunLighting the type of ambient sunlight and shadows in the TableTopSceneView.
 * @param ambientLightColor the color of the TableTopSceneView's ambient light.
 * @param onNavigationChanged lambda invoked when the navigation status of the TableTopSceneView has changed.
 * @param onSpatialReferenceChanged lambda invoked when the spatial reference of the TableTopSceneView has changed.
 * @param onLayerViewStateChanged lambda invoked when the TableTopSceneView's layer view state is changed.
 * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the TableTopSceneView.
 * @param onCurrentViewpointCameraChanged lambda invoked when the viewpoint camera of the TableTopSceneView has changed.
 * @param onRotate lambda invoked when a user performs a rotation gesture on the TableTopSceneView.
 * @param onScale lambda invoked when a user performs a pinch gesture on the TableTopSceneView.
 * @param onUp lambda invoked when the user removes all their pointers from the TableTopSceneView.
 * @param onDown lambda invoked when the user first presses on the TableTopSceneView.
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the TableTopSceneView.
 * @param onDoubleTap lambda invoked the user double taps on the TableTopSceneView.
 * @param onLongPress lambda invoked when a user holds a pointer on the TableTopSceneView.
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the TableTopSceneView.
 * @param onPan lambda invoked when a user drags a pointer or pointers across TableTopSceneView.
 * @param content the content of the TableTopSceneView.
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
    content: (@Composable TableTopSceneViewScope.() -> Unit)? = null
) {
    val initializationStatus = rememberTableTopSceneViewStatus()

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

    val arCoreInstalled by rememberArCoreInstalled(
        onFailed = {
            initializationStatus.update(
                TableTopSceneViewStatus.FailedToInitialize(it),
                onInitializationStatusChanged
            )
        }
    )

    val cameraController = remember {
        TransformationMatrixCameraController().apply {
            setOriginCamera(Camera(arcGISSceneAnchor, heading = 0.0, pitch = 90.0, roll = 0.0))
            setTranslationFactor(translationFactor)
            this.clippingDistance = clippingDistance ?: 0.0
        }
    }
    var arCoreAnchor: Anchor? by remember { mutableStateOf(null) }
    var visualizePlanes by remember { mutableStateOf(true) }

    rememberTextFieldState()

    Box(modifier = modifier) {
        if (cameraPermissionGranted && arCoreInstalled) {
            val arSessionWrapper =
                rememberArSessionWrapper(
                    applicationContext = context.applicationContext,
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                )
            SideEffect {
                // We need to check, otherwise during subsequent recompositions we could accidentally
                // revert from `Initialized` back to `DetectingPlanes`.
                if (initializationStatus.value is TableTopSceneViewStatus.Initializing) {
                    initializationStatus.update(
                        TableTopSceneViewStatus.DetectingPlanes,
                        onInitializationStatusChanged

                    )
                }
            }
            val identityMatrix = remember { TransformationMatrix.createIdentityMatrix() }
            ArCameraFeed(
                session = arSessionWrapper,
                onFrame = { frame, displayRotation, session ->
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
                        tableTopSceneViewProxy.sceneViewProxy.setFieldOfViewFromLensIntrinsics(
                            frame.camera,
                            displayRotation
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
            // Disable interaction, which is not supported in TableTop scenarios
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
                sceneViewProxy = tableTopSceneViewProxy.sceneViewProxy,
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
                    content?.invoke(TableTopSceneViewScope(this))
                }
            )
        }
    }
}
