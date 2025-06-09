/*
 COPYRIGHT 1995-2025 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

package com.arcgismaps.toolkit.ar

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyGraphicsOverlayResult
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.LayerViewState
import com.arcgismaps.mapping.view.LocationToScreenResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.toolkit.ar.internal.ArSessionWrapper
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.google.ar.core.Config

@Composable
public fun rememberFlyoverSceneViewProxy(
    initialLocation: Point,
    initialHeading: Double,
    translationFactor: Double
): FlyoverSceneViewProxy {
    return remember {
        FlyoverSceneViewProxy(
            location = initialLocation,
            heading = initialHeading,
            translationFactor = translationFactor
        )
    }
}

/**
 * Used to perform operations on a [FlyoverSceneView].
 *
 * There should be a one-to-one relationship between a FlyoverSceneViewProxy and a [FlyoverSceneView]. This
 * relationship is established by passing an instance of FlyoverSceneViewProxy to the [FlyoverSceneView] function.
 * Operations can only be performed once the associated composable FlyoverSceneView has entered the composition.
 * Operations performed when the associated composable FlyoverSceneView is not in the composition will fail gracefully,
 * i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.8.0
 */
public class FlyoverSceneViewProxy internal constructor(internal val sceneViewProxy: SceneViewProxy) {
    internal val cameraController = TransformationMatrixCameraController()

    /**
     * When instantiating this class from a composable, use [rememberFlyoverSceneViewProxy] to
     * automatically remember the proxy.
     *
     * @param location the starting location
     * @param heading the starting heading
     * @param translationFactor the translation factor that defines how much the scene view translates
     * as the device moves.
     *
     * @since 200.8.0
     */
    public constructor(
        location: Point,
        heading: Double,
        translationFactor: Double
    ) : this(SceneViewProxy()) {
        cameraController.setOriginCamera(
            Camera(
                locationPoint = location,
                pitch = 90.0,
                roll = 0.0,
                heading = heading
            )
        )
        cameraController.setTranslationFactor(translationFactor)
    }

    init {
        sceneViewProxy.setManualRenderingEnabled(true)
    }

    private var _sessionWrapper: ArSessionWrapper? = null

    internal fun setSessionWrapper(sessionWrapper: ArSessionWrapper?) {
        _sessionWrapper = sessionWrapper
        _sessionWrapper?.resetSession(planeFindingMode = Config.PlaneFindingMode.DISABLED)
    }

    /**
     * Sets the camera location and heading.
     *
     * @param location the starting location
     * @param heading the starting heading
     *
     * @since 200.8.0
     */
    public fun setLocationAndHeading(location: Point, heading: Double) {
        cameraController.setOriginCamera(
            Camera(
                locationPoint = location,
                pitch = 90.0,
                roll = 0.0,
                heading = heading
            )
        )
        _sessionWrapper?.resetSession(planeFindingMode = Config.PlaneFindingMode.DISABLED)
    }

    /**
     * Sets the translation factor that defines how much the scene view translates
     * as the device moves.
     *
     * @param translationFactor the translation factor
     *
     * @since 200.8.0
     */
    public fun setTranslationFactor(translationFactor: Double) {
        cameraController.setTranslationFactor(translationFactor)
    }

    /**
     * True if continuous panning across the international date line is enabled in the WorldScaleSceneView, false otherwise.
     * A null value represents that it is currently undetermined.
     *
     * @since 200.8.0
     */
    public val isWrapAroundEnabled: Boolean?
        get() = sceneViewProxy.isWrapAroundEnabled

    /**
     * Exports an image snapshot of the current WorldScaleSceneView.
     *
     * @return A [Result] containing a [BitmapDrawable], or failure
     * @since 200.8.0
     */
    public suspend fun exportImage(): Result<BitmapDrawable> = sceneViewProxy.exportImage()

    /**
     * Initiate an Identify operation on the specified [graphicsOverlay].
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any graphics
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance
     * of 0 tests just the physical pixel at [screenCoordinate].
     * Tolerance values above 0 are in [Dp] and specify a circular region centered on [screenCoordinate], with radius equal
     * to [tolerance]. The maximum allowed tolerance value is 100 Dp, resulting in an identify circle of diameter
     * 200 Dp.
     *
     * The [returnPopupsOnly] parameter controls what properties are populated in the [IdentifyGraphicsOverlayResult]
     * instance that is returned by the identify operation:
     * * true: only the [IdentifyGraphicsOverlayResult.popups] property will be populated with results. If the overlay
     * does not have popups an error will be returned.
     * * false: the [IdentifyGraphicsOverlayResult.graphics] property will be populated, and the
     * [IdentifyGraphicsOverlayResult.popups] property will be populated if the overlay has popups.
     *
     * @param graphicsOverlay overlay on which to run the identify
     * @param screenCoordinate location at which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the graphics property of the result is populated
     * @param maximumResults maximum size of the result set of graphics to return. A null value indicates unlimited results
     * @return A [Result] containing an [IdentifyGraphicsOverlayResult], or failure
     * @since 200.8.0
     */
    public suspend fun identify(
        graphicsOverlay: GraphicsOverlay,
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int? = 1
    ): Result<IdentifyGraphicsOverlayResult> = sceneViewProxy.identify(
        graphicsOverlay,
        screenCoordinate,
        tolerance,
        returnPopupsOnly,
        maximumResults
    )

    /**
     * Initiate an Identify operation on all graphics overlays.
     *
     * Results are returned in top-to-bottom order.
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any graphics that
     * intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of
     * 0 tests just the physical pixel at [screenCoordinate]. Tolerance values above 0 are in [Dp] and specify a
     * circular region centered on [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance
     * value is 100 Dp, resulting in an identify circle of diameter 200 Dp.
     *
     * The [returnPopupsOnly] parameter controls what properties are populated in the [IdentifyGraphicsOverlayResult]
     * instances that are returned by the identify operation:
     * * true: only [IdentifyGraphicsOverlayResult.popups] properties will be populated. Overlays
     * without popups will be omitted.
     * * false: [IdentifyGraphicsOverlayResult.graphics] properties will be populated. Overlays
     * with popups will also return popups.
     *
     * @param screenCoordinate location on which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the graphics property of the results are populated
     * @param maximumResults maximum size of the result set of graphics to return. A null value indicates unlimited results
     * @return A [Result] containing a [List] of [IdentifyGraphicsOverlayResult] containing one entry for each
     * overlay in the view, or failure. Each entry holds a [GraphicsOverlay] and a [List] of [com.arcgismaps.mapping.view.Graphic]s
     * @since 200.8.0
     */
    public suspend fun identifyGraphicsOverlays(
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int? = 1
    ): Result<List<IdentifyGraphicsOverlayResult>> = sceneViewProxy.identifyGraphicsOverlays(
        screenCoordinate,
        tolerance,
        returnPopupsOnly,
        maximumResults
    )

    /**
     * Initiate an Identify operation on the specified [layer].
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any GeoElements
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of
     * 0 tests just the physical pixel at [screenCoordinate]. Tolerance values above 0 are in [Dp] and specify a circular
     * region centered on [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance value is 100 Dp,
     * resulting in an identify circle of diameter 200 Dp. The [returnPopupsOnly] parameter controls what properties
     * are populated in the [IdentifyLayerResult] instance that is returned by the identify operation:
     *
     * * true: only the [IdentifyLayerResult.popups] property will be populated with results.
     * If the layer does not have popups an error will be returned.
     *
     * * false: the [IdentifyLayerResult.geoElements] property will be populated.
     * The [IdentifyLayerResult.popups] property will be populated if the layer has popups.
     *
     * @param layer layer on which to run the identify
     * @param screenCoordinate location at which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the [IdentifyLayerResult.geoElements] property of the result is populated
     * @param maximumResults maximum size of the result set of GeoElements (element type dependent on target layer) to
     * return per layer or sublayer. A null value indicates unlimited results
     * @return A [Result] containing an [IdentifyLayerResult], or failure
     * @since 200.8.0
     */
    public suspend fun identify(
        layer: Layer,
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int? = 1
    ): Result<IdentifyLayerResult> = sceneViewProxy.identify(
        layer,
        screenCoordinate,
        tolerance,
        returnPopupsOnly,
        maximumResults
    )

    /**
     * Initiate an Identify operation on all layers in the view.
     *
     * Results are returned in top-to-bottom order.
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any GeoElements
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of 0 tests just the
     * physical pixel at [screenCoordinate]. Tolerance values above 0 are in [Dp] and specify a circular region centered on
     * [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance value is 100 Dp, resulting in an
     * identify circle of diameter 200 Dp. The [returnPopupsOnly] parameter controls what properties are populated in
     * the [IdentifyLayerResult] instances that are returned by the identify operation:
     * * true: each [IdentifyLayerResult.popups] property will be populated with results. Layers without
     * popups will be omitted.
     * * false: GeoElements will be populated. Layers with popups will also return popups.
     *
     * @param screenCoordinate location on which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the [IdentifyLayerResult.geoElements] property of the results are populated
     * @param maximumResults maximum number of GeoElements to return per layer or sublayer. A null value indicates
     * unlimited results
     * @return A [Result] containing a [List] of [IdentifyLayerResult], containing one entry for each layer in the
     * view that supports identify, or failure. Each entry contains a [Layer] and a [List] of elements of the type
     * contained by the layer (e.g. [com.arcgismaps.data.Feature] for an [com.arcgismaps.mapping.layers.FeatureLayer])
     * @since 200.8.0
     */
    public suspend fun identifyLayers(
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int? = 1
    ): Result<List<IdentifyLayerResult>> =
        sceneViewProxy.identifyLayers(screenCoordinate, tolerance, returnPopupsOnly, maximumResults)

    /**
     * Retrieve the layer's [LayerViewState].
     *
     * @param layer the layer to retrieve the view state from
     * @return the [LayerViewState] of the provided layer, or null if this proxy's WorldScaleSceneView is not
     * part of the composition
     * @since 200.8.0
     */
    public fun getLayerViewState(layer: Layer): LayerViewState? =
        sceneViewProxy.getLayerViewState(layer)

    /**
     * Converts a location in map coordinates to a point in screen coordinates relative to the upper-left corner of the WorldScaleSceneView.
     *
     * The screen coordinates are in device-independent pixels (DIP) relative to the upper-left corner of the scene
     * view at position 0,0. The [LocationToScreenResult] indicates whether the screen coordinates are visible
     * in the scene view. They can be invisible if they are on the other side of the globe, not in the
     * [com.arcgismaps.mapping.Viewpoint], or are blocked by the base surface or elevation layer.
     *
     * To call this method, assign a scene to the WorldScaleSceneView, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * @param point a location defined within the spatial reference of the scene view
     * @return A location to screen result object. If an error occurs, null is returned.
     * @since 200.8.0
     */
    public fun locationToScreen(point: Point): LocationToScreenResult? =
        sceneViewProxy.locationToScreen(point)

    /**
     * Asynchronously converts a screen coordinate relative to the upper-left corner of the scene view to a location in map coordinates.
     *
     * This is a high performance calculation executed on the GPU using a triangular mesh. Note that elevation
     * values are approximated, and as the distance between the camera and the surface increases, the precision of
     * the elevation value decreases.
     *
     * To call this method, assign a scene to the WorldScaleSceneView, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * @param screenCoordinate a pixel coordinate relative to the upper-left corner of the screen.
     * @return A [Result] containing a [Point] or an exception.
     * @since 200.8.0
     */
    public suspend fun screenToLocation(screenCoordinate: ScreenCoordinate): Result<Point> =
        sceneViewProxy.screenToLocation(screenCoordinate)

    /**
     * Converts a screen coordinate (in pixels) to a point on the base surface of the scene within the WorldScaleSceneView's spatial reference.
     *
     * To call this method, assign a scene to the WorldScaleSceneView, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * May return null in some circumstances, such as if the WorldScaleSceneView's spatial reference has not been determined yet.
     *
     * @param screenCoordinate the screen point, in pixels
     * @return a [Point] object, or null if the location could not be determined
     * @since 200.8.0
     */
    public fun screenToBaseSurface(screenCoordinate: ScreenCoordinate): Point? =
        sceneViewProxy.screenToBaseSurface(screenCoordinate)

    /**
     * The horizontal field of view of the WorldScaleSceneView in degrees.
     *
     * The value of the field of view is influenced by the size and orientation of the device screen.
     * A landscape orientation has a larger field of view value than a portrait orientation.
     *
     * A null value represents that it is currently undetermined.
     *
     * @since 200.8.0
     */
    public val fieldOfView: Double?
        get() = sceneViewProxy.fieldOfView

    /**
     * The ratio indicates how much the vertical field of view is distorted.
     *
     * A distortion factor less than 1.0 causes the visuals to be stretched taller in comparison to their width.
     * A distortion factor greater than 1.0 causes the visuals to be shrunk shorter in comparison to their width.
     *
     * The default value is 1.0.
     * A null value represents that it is currently undetermined.
     * @since 200.8.0
     */
    public val fieldOfViewDistortionRatio: Double?
        get() = sceneViewProxy.fieldOfViewDistortionRatio
}
