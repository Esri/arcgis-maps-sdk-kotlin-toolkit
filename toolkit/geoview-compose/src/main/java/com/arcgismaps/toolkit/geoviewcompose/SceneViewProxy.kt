/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DeviceOrientation
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.LocationToScreenResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Used to perform operations on a composable [SceneView].
 *
 * There should be a one-to-one relationship between a SceneViewProxy and a composable [SceneView]. This
 * relationship is established by passing an instance of SceneViewProxy to the composable [SceneView] function.
 * Operations can only be performed once the associated composable SceneView has entered the composition.
 * Operations performed when the associated composable SceneView is not in the composition will fail gracefully,
 * i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.4.0
 */
@Stable
public class SceneViewProxy : GeoViewProxy("SceneView") {

    private var _isManualRenderingEnabled = false

    /**
     * The view-based [com.arcgismaps.mapping.view.SceneView] that this SceneViewProxy will operate on. This should
     * be initialized by the composable [SceneView] when it enters the composition and set to null when
     * it is disposed by calling [setSceneView].
     *
     * @since 200.4.0
     */
    private var sceneView: com.arcgismaps.mapping.view.SceneView? = null
        set(value) {
            setGeoView(value)
            field = value

            // sync up the manual rendering state as it may have been set before the SceneView was
            // initialized
            value?.isManualRenderingEnabled = _isManualRenderingEnabled
        }

    /**
     * Sets the [sceneView] parameter on this operator. This should be called by the composable [SceneView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.4.0
     */
    internal fun setSceneView(sceneView: com.arcgismaps.mapping.view.SceneView?) {
        this.sceneView = sceneView
    }

    private val nullSceneViewErrorMessage: String =
        "SceneView must be part of the composition when this member is called."

    /**
     * Converts a location in map coordinates to a point in screen coordinates relative to the upper-left corner of the scene view.
     *
     * The screen coordinates are in device-independent pixels (DIP) relative to the upper-left corner of the scene
     * view at position 0,0. The [LocationToScreenResult] indicates whether the screen coordinates are visible
     * in the scene view. They can be invisible if they are on the other side of the globe, not in the
     * [com.arcgismaps.mapping.Viewpoint], or are blocked by the base surface or elevation layer.
     *
     * To call this method, assign a scene to the scene view, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * @param point a location defined within the spatial reference of the scene view
     * @return A location to screen result object. If an error occurs, null is returned.
     * @since 200.4.0
     */
    public fun locationToScreen(point: Point): LocationToScreenResult? =
        sceneView?.locationToScreen(point)

    /**
     * Asynchronously converts a screen coordinate relative to the upper-left corner of the scene view to a location in map coordinates.
     *
     * This is a high performance calculation executed on the GPU using a triangular mesh. Note that elevation
     * values are approximated, and as the distance between the camera and the surface increases, the precision of
     * the elevation value decreases.
     *
     * To call this method, assign a scene to the scene view, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * @param screenCoordinate a pixel coordinate relative to the upper-left corner of the screen.
     * @return A [Result] containing a [Point] or an exception.
     * @since 200.4.0
     */
    public suspend fun screenToLocation(screenCoordinate: ScreenCoordinate): Result<Point> =
        sceneView?.screenToLocation(screenCoordinate) ?: Result.failure(
            IllegalStateException(
                nullSceneViewErrorMessage
            )
        )

    /**
     * Converts a screen coordinate (in pixels) to a point on the base surface of the scene within the scene view's spatial reference.
     *
     * To call this method, assign a scene to the scene view, ensure that it is loaded and the draw status is
     * [DrawStatus.Completed].
     *
     * May return null in some circumstances, such as if the scene view's spatial reference has not been determined yet.
     *
     * @param screenCoordinate the screen point, in pixels
     * @return a [Point] object, or null if the location could not be determined
     * @since 200.4.0
     */
    public fun screenToBaseSurface(screenCoordinate: ScreenCoordinate): Point? =
        sceneView?.screenToBaseSurface(screenCoordinate)

    /**
     * The horizontal field of view of the scene view in degrees.
     *
     * The value of the field of view is influenced by the size and orientation of the device screen.
     * A landscape orientation has a larger field of view value than a portrait orientation.
     *
     * A null value represents that it is currently undetermined.
     *
     * @since 200.4.0
     */
    public val fieldOfView: Double?
        get() = sceneView?.fieldOfView

    /**
     * The ratio indicates how much the vertical field of view is distorted.
     *
     * A distortion factor less than 1.0 causes the visuals to be stretched taller in comparison to their width.
     * A distortion factor greater than 1.0 causes the visuals to be shrunk shorter in comparison to their width.
     *
     * The default value is 1.0.
     * A null value represents that it is currently undetermined.
     * @since 200.4.0
     */
    public val fieldOfViewDistortionRatio: Double?
        get() = sceneView?.fieldOfViewDistortionRatio

    /**
     * Indicates whether manual rendering is enabled or not.
     *
     * A null value indicates that the composable SceneView is not currently initialized.
     *
     * @see setManualRenderingEnabled
     * @since 200.4.0
     */
    public val isManualRenderingEnabled: Boolean
        get() = _isManualRenderingEnabled

    /**
     * Sets whether manual rendering is enabled or not.
     *
     * If set to true, the SceneView won't use its own pulse mechanism to draw the scene. The user of
     * the SceneView will have to call renderFrame to cause the SceneView to draw.
     *
     * Note that once this property has been set to true on a SceneView it is not possible to revert
     * back to the default pulse mechanism. If you intend to set this property to true for a SceneView
     * then it is advisable to do so as soon as possible after the SceneView has been initialized
     *
     * @since 200.4.0
     */
    public fun setManualRenderingEnabled(isManualRenderingEnabled: Boolean) {
        _isManualRenderingEnabled = isManualRenderingEnabled
        sceneView?.isManualRenderingEnabled = isManualRenderingEnabled
    }

    /**
     * Draws the scene based on a user-defined pulse.
     * In order for this method to have any effect, [isManualRenderingEnabled] has to be set to true
     *
     * @see setManualRenderingEnabled
     * @since 200.4.0
     */
    public fun renderFrame() {
        sceneView?.renderFrame()
    }

    /**
     * Sets the field of view on the scene view in degrees and determines how much the vertical field of view is distorted.
     *
     * A distortion ratio less than 1.0 will cause the visuals to be stretched taller in comparison to their
     * width. A distortion ratio greater than 1.0 will cause the visuals to be shrunk shorter in comparison to
     * their width.
     *
     * The default distortion ratio is 1.0.
     *
     * @param angle the field of view on the scene view in degrees. This value must be greater than 0 and less than or equal to 120
     * @param distortionRatio the field of view vertical distortion ratio. This value must be between 0.1 and 10
     * @since 200.4.0
     */
    public fun setFieldOfView(angle: Double, distortionRatio: Double = 1.0) {
        sceneView?.setFieldOfViewAndDistortionRatio(angle, distortionRatio)
    }

    /**
     * Matches the field of view of the scene view to the field of view of a camera lens using the lens characteristics.
     * All parameter values must be greater than 0.
     *
     * @param xFocalLength the pixel focal length along the x-axis. The units are in pixels.
     * xFocal and yFocal should be identical for square pixels
     * @param yFocalLength the pixel focal length along the y-axis. The units are in pixels.
     * xFocal and yFocal should be identical for square pixels
     * @param xPrincipal the distance along the x-axis between the principal point and the top-left corner of the
     * image frame. The units are in pixels. This must also be less than xImageSize
     * @param yPrincipal the distance along the y-axis between the principal point and the top-left corner of the
     * image frame. The units are in pixels. This must also be less than yImageSize.
     * @param xImageSize the x value of the image size captured by the camera. The units are in pixels
     * @param yImageSize the y value of the image size captured by the camera. The units are in pixels
     * @param deviceOrientation the orientation of the device
     * @since 200.4.0
     */
    public fun setFieldOfViewFromLensIntrinsics(
        xFocalLength: Float,
        yFocalLength: Float,
        xPrincipal: Float,
        yPrincipal: Float,
        xImageSize: Float,
        yImageSize: Float,
        deviceOrientation: DeviceOrientation
    ) {
        sceneView?.setFieldOfViewFromLensIntrinsics(
            xFocalLength,
            yFocalLength,
            xPrincipal,
            yPrincipal,
            xImageSize,
            yImageSize,
            deviceOrientation
        )
    }

    /**
     * Change the scene view to the viewpoint specified by the given camera.
     * The viewpoint is updated instantaneously.
     *
     * @param camera the new camera
     * @since 200.4.0
     */
    public fun setViewpointCamera(camera: Camera) {
        sceneView?.setViewpointCamera(camera)
    }

    /**
     * Animate the scene view to the viewpoint specified by the given camera using the specified duration.
     *
     * @param camera the new camera
     * @param duration the duration of the animation
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointCameraAnimated(
        camera: Camera,
        duration: Duration = 0.25.seconds
    ): Result<Boolean> {
        return sceneView?.setViewpointCameraAnimated(
            camera,
            duration.toDouble(DurationUnit.SECONDS).toFloat()
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }
}
