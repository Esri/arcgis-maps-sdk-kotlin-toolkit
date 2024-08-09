package com.arcgismaps.toolkit.ar

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.TimeExtent
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.CameraController
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
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SceneViewInteractionOptions
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import com.arcgismaps.mapping.view.ViewLabelProperties
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewDefaults
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewScope
import java.time.Instant


@Composable
public fun TabletopArView2(sceneViewProxy: SceneViewProxy = remember { SceneViewProxy() }, content: @Composable (tabletopArViewState: TabletopArViewState) -> Unit) {
    val tabletopArViewState = remember { TabletopArViewState(sceneViewProxy) }
    Box {
        content(tabletopArViewState)
    }
}

public data class TabletopArViewState(
    public val sceneViewProxy: SceneViewProxy,
    public val cameraController: CameraController = TransformationMatrixCameraController(),
    public val atmosphereEffect: AtmosphereEffect = AtmosphereEffect.None,
    public val spaceEffect: SpaceEffect = SpaceEffect.Transparent
) {
    internal fun onSpatialReferenceChanged(spatialReference: SpatialReference?) {
        if (spatialReference != null) {
            sceneViewProxy.setManualRenderingEnabled(true)
        }
    }
}

@Composable
public fun SceneView(
    arcGISScene: ArcGISScene,
    tabletopArViewState: TabletopArViewState,
    modifier: Modifier = Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
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
    content: (@Composable SceneViewScope.() -> Unit)? = null
): Unit = SceneView(
    arcGISScene = arcGISScene,
    modifier = modifier,
    onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
    onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
    graphicsOverlays = graphicsOverlays,
    sceneViewProxy = tabletopArViewState.sceneViewProxy,
    sceneViewInteractionOptions = sceneViewInteractionOptions,
    viewLabelProperties = viewLabelProperties,
    selectionProperties = selectionProperties,
    isAttributionBarVisible = isAttributionBarVisible,
    onAttributionTextChanged = onAttributionTextChanged,
    onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
    cameraController = tabletopArViewState.cameraController,
    analysisOverlays = analysisOverlays,
    imageOverlays = imageOverlays,
    atmosphereEffect = tabletopArViewState.atmosphereEffect,
    timeExtent = timeExtent,
    onTimeExtentChanged = onTimeExtentChanged,
    spaceEffect = tabletopArViewState.spaceEffect,
    sunTime = sunTime,
    sunLighting = sunLighting,
    ambientLightColor = ambientLightColor,
    onNavigationChanged = onNavigationChanged,
    onSpatialReferenceChanged = {
        tabletopArViewState.onSpatialReferenceChanged(it)
        if (onSpatialReferenceChanged != null) {
            onSpatialReferenceChanged(it)
        }
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
    content = content
)

@Composable
public fun TabletopArView(sceneViewProxy: SceneViewProxy = remember { SceneViewProxy() }, content: @Composable TabletopArScope.() -> Unit) {
    val tabletopArScope = remember { TabletopArScope(sceneViewProxy) }
    Box {
        tabletopArScope.content()
    }
}

public data class TabletopArScope(
    public val sceneViewProxy: SceneViewProxy,
    public val cameraController: CameraController = TransformationMatrixCameraController(),
    public val atmosphereEffect: AtmosphereEffect = AtmosphereEffect.None,
    public val spaceEffect: SpaceEffect = SpaceEffect.Transparent
)

@Composable
public fun TabletopArScope.SceneView(
    arcGISScene: ArcGISScene,
    modifier: Modifier = Modifier,
    onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)? = null,
    onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)? = null,
    graphicsOverlays: List<GraphicsOverlay> = remember { emptyList() },
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
    content: (@Composable SceneViewScope.() -> Unit)? = null
) {
    SceneView(
        arcGISScene = arcGISScene,
        modifier = modifier,
        onViewpointChangedForCenterAndScale = onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry = onViewpointChangedForBoundingGeometry,
        graphicsOverlays = graphicsOverlays,
        sceneViewProxy = this.sceneViewProxy,
        sceneViewInteractionOptions = sceneViewInteractionOptions,
        viewLabelProperties = viewLabelProperties,
        selectionProperties = selectionProperties,
        isAttributionBarVisible = isAttributionBarVisible,
        onAttributionTextChanged = onAttributionTextChanged,
        onAttributionBarLayoutChanged = onAttributionBarLayoutChanged,
        cameraController = this.cameraController,
        analysisOverlays = analysisOverlays,
        imageOverlays = imageOverlays,
        atmosphereEffect = this.atmosphereEffect,
        timeExtent = timeExtent,
        onTimeExtentChanged = onTimeExtentChanged,
        spaceEffect = this.spaceEffect,
        sunTime = sunTime,
        sunLighting = sunLighting,
        ambientLightColor = ambientLightColor,
        onNavigationChanged = onNavigationChanged,
        onSpatialReferenceChanged = {
            if (it != null) {
                sceneViewProxy.setManualRenderingEnabled(true)
            }
            if (onSpatialReferenceChanged != null) {
                onSpatialReferenceChanged(it)
            }
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
        content = content
    )
}