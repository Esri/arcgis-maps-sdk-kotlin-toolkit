package com.arcgismaps.toolkit.ar.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewScope
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun WorldTrackingSceneView(
    arcGISScene: ArcGISScene,
    onInitializationStatusChanged: ((WorldScaleSceneViewStatus) -> Unit),
    modifier: Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)?,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)?,
    graphicsOverlays: List<GraphicsOverlay>,
    worldScaleSceneViewProxy: WorldScaleSceneViewProxy,
    viewLabelProperties: ViewLabelProperties,
    selectionProperties: SelectionProperties,
    isAttributionBarVisible: Boolean,
    onAttributionTextChanged: ((String) -> Unit)?,
    onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)?,
    analysisOverlays: List<AnalysisOverlay>,
    imageOverlays: List<ImageOverlay>,
    timeExtent: TimeExtent?,
    onTimeExtentChanged: ((TimeExtent?) -> Unit)?,
    sunTime: Instant,
    sunLighting: LightingMode,
    ambientLightColor: Color,
    onNavigationChanged: ((isNavigating: Boolean) -> Unit)?,
    onSpatialReferenceChanged: ((spatialReference: SpatialReference?) -> Unit)?,
    onLayerViewStateChanged: ((GeoView.GeoViewLayerViewStateChanged) -> Unit)?,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)?,
    onCurrentViewpointCameraChanged: ((camera: Camera) -> Unit)?,
    onRotate: ((RotationChangeEvent) -> Unit)?,
    onScale: ((ScaleChangeEvent) -> Unit)?,
    onUp: ((UpEvent) -> Unit)?,
    onDown: ((DownEvent) -> Unit)?,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)?,
    onDoubleTap: ((DoubleTapEvent) -> Unit)?,
    onLongPress: ((LongPressEvent) -> Unit)?,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)?,
    onPan: ((PanChangeEvent) -> Unit)?,
    content: (@Composable WorldScaleSceneViewScope.() -> Unit)?
) {
    val arSessionWrapper =
        rememberArSessionWrapper(applicationContext = LocalContext.current.applicationContext)


    val localLifecycleOwner = LocalLifecycleOwner.current
    val calibrationState = remember { CalibrationState() }

    val locationTracker = rememberWorldTrackingCameraController(
        calibrationState = calibrationState,
        onLocationDataSourceFailedToStart = {
            onInitializationStatusChanged(WorldScaleSceneViewStatus.FailedToInitialize(it))
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
        onInitializationStatusChanged(WorldScaleSceneViewStatus.Initialized)

        // Don't display the scene view if the camera has not been set up yet, or else a globe will appear
        if (!locationTracker.hasSetOriginCamera) return@WorldTrackingSceneView

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
