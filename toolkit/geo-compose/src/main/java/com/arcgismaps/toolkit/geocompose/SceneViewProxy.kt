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

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.LocationToScreenResult
import com.arcgismaps.mapping.view.ScreenCoordinate

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
public class SceneViewProxy : GeoViewProxy("SceneView") {
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
    public fun locationToScreen(point: Point): LocationToScreenResult? = sceneView?.locationToScreen(point)

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
        sceneView?.screenToLocation(screenCoordinate) ?: Result.failure(IllegalStateException(nullSceneViewErrorMessage))

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

}
