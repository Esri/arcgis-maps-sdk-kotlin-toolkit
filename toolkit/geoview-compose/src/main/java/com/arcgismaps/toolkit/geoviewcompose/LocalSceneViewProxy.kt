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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LocalSceneView
import com.arcgismaps.mapping.view.LocationToScreenResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Used to perform operations on a composable [LocalSceneView].
 *
 * There should be a one-to-one relationship between a LocalSceneViewProxy and a composable
 * [LocalSceneView]. This relationship is established by passing an instance of LocalSceneViewProxy
 * to the composable [LocalSceneView] function. Operations can only be performed once the associated
 * composable LocalSceneView has entered the composition. Operations performed when the associated
 * composable LocalSceneView is not in the composition will fail gracefully, i.e. won't throw
 * exceptions but won't return a successful result.
 *
 * @since 300.0.0
 */
@Stable
public class LocalSceneViewProxy : GeoViewProxy("LocalSceneView") {
    /**
     * The view-based [com.arcgismaps.mapping.view.LocalSceneView] that this LocalSceneViewProxy
     * will operate on. This should be initialized by the composable [LocalSceneView] when it enters
     * the composition and set to null when it is disposed by calling [setLocalSceneView].
     *
     * @since 300.0.0
     */
    private var localSceneView: LocalSceneView? = null
        set(value) {
            setGeoView(value)
            field = value
        }

    /**
     * Sets the [localSceneView] parameter on this operator. This should be called by the composable
     * [LocalSceneView] when it enters the composition and set to null when it is disposed.
     *
     * @since 300.0.0
     */
    internal fun setLocalSceneView(localSceneView: LocalSceneView?) {
        this.localSceneView = localSceneView
    }

    private val nullSceneViewErrorMessage: String =
        "LocalSceneView must be part of the composition when this member is called."

    /**
     * Pans or zooms the local scene view using animation to the specified viewpoint location. The
     * animation takes place over the specified duration. The animation curve defines the animation
     * easing function.
     *
     * @param viewpoint The viewpoint that should be set on the view.
     * @param animationCurve The type of animation curve.
     * @param duration The amount of time to move to the new viewpoint.
     * @return a [Result] of true if the viewpoint was successfully set, false otherwise.
     * @since 300.0.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        animationCurve: AnimationCurve,
        duration: Duration = 3.seconds
    ): Result<Boolean> {
        return localSceneView?.setViewpointAnimated(
            viewpoint,
            duration.toDouble(DurationUnit.SECONDS).toFloat(),
            animationCurve
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Changes the display to the viewpoint specified by the given camera.
     *
     * @param camera The camera that should be set on the view.
     * @since 300.0.0
     */
    public fun setViewpointCamera(camera: Camera) {
        localSceneView?.setViewpointCamera(camera)
    }

    /**
     * Pans or zooms the local scene view using animation to the specified camera location
     * asynchronously. Animation takes place over the specified duration.
     *
     * @param camera The camera that should be set on the view.
     * @param duration The amount of time to move to the new viewpoint.
     * @return a [Result] of true if the viewpoint was successfully set, false otherwise.
     * @since 300.0.0
     */
    public suspend fun setViewpointCameraAnimated(
        camera: Camera,
        duration: Duration = 3.seconds
    ): Result<Boolean> {
        return localSceneView?.setViewpointCameraAnimated(
            camera,
            duration.toDouble(DurationUnit.SECONDS).toFloat()
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Converts the specified screen coordinate, relative to the upper-left corner of the local scene
     * view, to a location on the base surface in geographic coordinates.
     * Note that the elevation value for the converted location is approximated, as the precision
     * of the elevation value decreases with increasing distance between the camera and the surface.
     *
     * This method returns null if the provided screen coordinate is outside the bounds of the current
     * screen or if its location does not intersect with the surface of the local scene.
     *
     * To call this method, assign a local scene to the local scene view and ensure that it is loaded.
     *
     * @param screenCoordinate The screen coordinate to convert to a location on the base surface. The coordinate
     * of the top left corner of the screen is 0,0.
     *
     * @return A point on the base surface.
     * @since 300.0.0
     */
    public fun screenToBaseSurface(screenCoordinate: ScreenCoordinate): Point? =
        localSceneView?.screenToBaseSurface(screenCoordinate)

    /**
     * Asynchronously converts a screen coordinate, relative to the upper-left corner of the
     * LocalSceneView, to a location in scene coordinates.
     *
     * This calculation is executed on the GPU using a triangular mesh. Note that elevation values
     * are approximated, and as the distance between the camera and the surface increases, the
     * precision of the elevation value decreases.
     *
     * If the provided screen coordinates are outside of the bounds of the current screen, this
     * method will immediately fail with an error.
     *
     * If the provided screen coordinates do not intersect with the surface of the local scene, the
     * returned point will be empty.
     *
     * @param screenCoordinate The screen coordinate. The coordinate of the top left corner of the
     * screen is 0,0.
     *
     * @since 300.0.0
     */
    public suspend fun screenToLocation(screenCoordinate: ScreenCoordinate): Result<Point> =
        localSceneView?.screenToLocation(screenCoordinate) ?: Result.failure(
            IllegalStateException(
                nullSceneViewErrorMessage
            )
        )

    /**
     * Converts a location in map coordinates to a point in screen coordinates relative to the
     * upper-left corner of the local scene view.
     *
     * The screen coordinates are in device-independent pixels (DIP) relative to the upper-left corner
     * of the scene view at position 0,0. The [LocationToScreenResult] indicates whether the result
     * screen point is visible in the local scene view. It will not be visible if it is outside the
     * applied clipping area, is not within the current [Viewpoint], or is blocked by the base
     * surface or elevation layer.
     *
     * If the input point does not have a spatial reference, it is assumed to be in the view's spatial
     * reference.
     *
     * If the input point's spatial reference is not the same as the view's, it is projected to the
     * view's spatial reference. If the projection fails, then null is returned.
     *
     * To call this method, assign a local scene to the local scene view and ensure that it is loaded.
     *
     * @param point A location defined within the spatial reference of the local scene view.
     *
     * @return A location to screen result object. If an error occurs, null is returned.
     *
     * @since 300.0.0
     */
    public fun locationToScreen(point: Point): LocationToScreenResult? =
        localSceneView?.locationToScreen(point)
}
