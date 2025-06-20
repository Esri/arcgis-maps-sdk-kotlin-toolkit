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
     * Initiate an Identify operation on the mapView's [geometryEditor], if any, to return GeometryEditor elements.
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. This overload
     * will return the visible topmost graphic. A tolerance of 0 tests just the physical pixel at [screenCoordinate].
     * Tolerance values above 0 are in DIPs and specify a circular region centered on [screenCoordinate], with radius equal
     * to [tolerance]. The maximum allowed tolerance value is 100 DIPs, resulting in an identify circle of diameter
     * 200 DIPs.
     *
     * @param screenCoordinate location at which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @return A [Result] containing an [IdentifyGeometryEditorResult], or failure if there is no [geometryEditor].
     * GeometryEditorElement results are returned in the same order as on the [geometryEditor] display; that is top-first order,
     * for example with GeometryEditorVertex first.
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
