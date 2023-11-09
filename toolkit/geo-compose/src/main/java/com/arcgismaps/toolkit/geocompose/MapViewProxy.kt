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
import com.arcgismaps.mapping.view.ScreenCoordinate


/**
 * Used to perform operations on a [MapView].
 *
 * There should be a one-to-one relationship between a MapViewProxy and a composable [MapView]. This relationship is esatblished by passing an instance of MapViewProxy to the composable [MapView] function.
 * Operations can only be performed once the associated composable MapView has entered the composition. Operations performed when the associated composable MapView is not in the composition will fail gracefully, i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.3.0
 */
public class MapViewProxy : GeoViewProxy() {

    /**
     * The [com.arcgismaps.mapping.view.MapView] that this MapViewProxy will operate on. This should be initialized by the composable [MapView]
     * composable when it enters the composition and set to null when it is disposed by calling [setMapView].
     *
     * @since 200.3.0
     */
    private var mapView: com.arcgismaps.mapping.view.MapView? = null

    /**
     * Sets the [mapView] parameter on this operator. This should be called by the composable [MapView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.3.0
     */
    internal fun setMapView(mapView: com.arcgismaps.mapping.view.MapView?) {
        this.mapView = mapView
    }

    /**
     * Converts a screen coordinate (in pixels) to a coordinate within the map view's spatial reference.
     *
     * May return null in some circumstances, such as if the map view's spatial reference has not been determined yet.
     *
     * @param screenCoordinate the screen point, in pixels
     * @return a [Point] object, or null if the location could not be determined or an error occurs
     * @since 200.3.0
     */
    public fun screenToLocationOrNull(screenCoordinate: ScreenCoordinate): Point? {
        return try {
             mapView?.screenToLocation(screenCoordinate)
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Converts a coordinate within the map view's spatial reference to a screen coordinate (in pixels).
     * If the wraparound mode is active, this method returns the closest screen location matching the
     * specified map location. 'Closest' meaning: If it's in view, return that location, otherwise return
     * for the frame where the location is the closest to the center of the view.
     *
     * @param mapPoint a [Point] object representing a coordinate on the map
     * @return A [ScreenCoordinate] for the screen in pixels. Returns null if an error occurs
     * @since 200.3.0
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
}
