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
import androidx.compose.ui.unit.Dp
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.mapping.view.IdentifyGeometryEditorResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor

/**
 * Used to perform operations on a composable [MapView].
 *
 * There should be a one-to-one relationship between a MapViewProxy and a composable [MapView]. This
 * relationship is established by passing an instance of MapViewProxy to the composable [MapView] function.
 * Operations can only be performed once the associated composable MapView has entered the composition.
 * Operations performed when the associated composable MapView is not in the composition will fail gracefully,
 * i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.4.0
 */
@Stable
public class MapViewProxy : GeoViewProxy("MapView") {

    /**
     * The view-based [com.arcgismaps.mapping.view.MapView] that this MapViewProxy will operate on. This should
     * be initialized by the composable [MapView] when it enters the composition and set to null when
     * it is disposed by calling [setMapView].
     *
     * @since 200.4.0
     */
    private var mapView: com.arcgismaps.mapping.view.MapView? = null
        set(value) {
            setGeoView(value)
            field = value
        }

    /**
     * Sets the [mapView] parameter on this operator. This should be called by the composable [MapView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.4.0
     */
    internal fun setMapView(mapView: com.arcgismaps.mapping.view.MapView?) {
        this.mapView = mapView
    }

    /**
     * Converts a screen coordinate (in pixels) to a coordinate within the mapview's spatial reference.
     *
     * May return null in some circumstances, such as if the mapview's spatial reference has not been
     * determined yet.
     *
     * @param screenCoordinate the screen point, in pixels
     * @return a [Point] object, or null if the location could not be determined or an error occurs
     * @since 200.4.0
     */
    public fun screenToLocationOrNull(screenCoordinate: ScreenCoordinate): Point? {
        return try {
            mapView?.screenToLocation(screenCoordinate)
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Converts a coordinate within the mapview's spatial reference to a screen coordinate (in pixels).
     * If the wraparound mode is active, this method returns the closest screen location matching the
     * specified map location. 'Closest' meaning: If it's in view, return that location, otherwise return
     * for the frame where the location is the closest to the center of the view.
     *
     * @param mapPoint a [Point] object representing a coordinate on the map
     * @return A [ScreenCoordinate] for the screen in pixels. Returns null if an error occurs
     * @since 200.4.0
     */
    public fun locationToScreenOrNull(mapPoint: Point): ScreenCoordinate? {
        return try {
            mapView?.locationToScreen(mapPoint)?.let {
                if (it.x.isNaN() || it.y.isNaN()) null
                else it
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Identifies all the elements in the [GeometryEditor], at the given screen point.
     *
     * As locations from user gestures are not always accurate to the exact pixel, you can define a tolerance for
     * the identify operation. The tolerance parameter sets the radius of a circle, centered at the specified
     * coordinates, in device-independent pixels (DIP). If the tolerance value is 0, identify performs the test at
     * the specified coordinate. If it is greater than 0, identify tests completely within the circle. For touch
     * displays a value of 22 is recommended to cover an average finger tap. The maximum allowed value is 100 DIPs.
     *
     * The default tolerance values used by the geometry editor for each input type are:
     * * Mouse device - 5 DIPs.
     * * Stylus device - 10 DIPs.
     * * Touch - 15 DIPs.
     * * Reticle tool - 5 DIPs.
     *
     * This operation will fail if:
     * * No [GeometryEditor] is attached to the [MapView].
     * * The attached [GeometryEditor] is stopped.
     *
     * @param screenCoordinate to identify the geometry editor elements.
     * @param tolerance radius in device-independent pixels (DIP) that specifies how precise the identify
     * operation should be.
     * @return A [Result] of [IdentifyGeometryEditorResult] containing an array of [GeometryEditorElement]
     * or failure if there is no [GeometryEditor].
     * Results are returned in the same order as the draw order of [GeometryEditorElement]s on the
     * [GeometryEditor] display; that is top-first order, with [GeometryEditorVertex] appearing drawn
     * on-top of other elements.
     * @since 200.8.0
     */
    public suspend fun identifyGeometryEditor(
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp
    ): Result<IdentifyGeometryEditorResult> {
        return mapView?.identifyGeometryEditor(
            screenCoordinate,
            tolerance.value.toDouble(),
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Animates the map view to the new viewpoint, taking the given duration to complete the navigation.
     *
     * @param viewpoint the new viewpoint
     * @param duration the duration of the animation
     * @param curve the animation curve to apply
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        duration: Duration,
        curve: AnimationCurve
    ): Result<Boolean> {
        return mapView?.setViewpointAnimated(
            viewpoint,
            duration.toDouble(DurationUnit.SECONDS).toFloat(),
            curve
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Animate the map view to the center point and scale.
     *
     * @param center the location on which the map should be centered
     * @param scale the new map scale
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointCenter(center: Point, scale: Double? = null): Result<Boolean> {
        return if (scale != null) {
            mapView?.setViewpointCenter(center, scale)
        } else {
            mapView?.setViewpointCenter(center)
        } ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Animate the map view to the bounding geometry with padding applied.
     *
     * @param boundingGeometry the geometry to zoom to. If the spatial reference of the geometry is
     * different to that of the composable [MapView], it will be reprojected appropriately
     * @param paddingInDips a distance around the geometry to include in the Viewpoint when zooming,
     * in density-independent pixels
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointGeometry(
        boundingGeometry: Geometry,
        paddingInDips: Double? = null
    ): Result<Boolean> {
        return if (paddingInDips != null) {
            mapView?.setViewpointGeometry(boundingGeometry, paddingInDips)
        } else {
            mapView?.setViewpointGeometry(boundingGeometry)
        } ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Animate the rotation of the map view to the provided angle.
     *
     * @param angleDegrees the new map rotation angle, in degrees counter-clockwise
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointRotation(angleDegrees: Double): Result<Boolean> {
        return mapView?.setViewpointRotation(angleDegrees) ?: Result.failure(
            IllegalStateException(
                nullGeoViewErrorMessage
            )
        )
    }

    /**
     * Animate the map view to zoom to a scale.
     *
     * @param scale the new map scale
     * @return a [Result] indicating whether the viewpoint was successfully set.
     * A success result with a value of false may indicate the operation was cancelled.
     * @since 200.4.0
     */
    public suspend fun setViewpointScale(scale: Double): Result<Boolean> {
        return mapView?.setViewpointScale(scale) ?: Result.failure(
            IllegalStateException(
                nullGeoViewErrorMessage
            )
        )
    }
}
