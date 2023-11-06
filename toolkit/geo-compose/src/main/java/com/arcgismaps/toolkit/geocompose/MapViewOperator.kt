package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

public class MapViewOperator() {
    private var mapView: MapView? = null
    private val lock = Unit
    internal fun setMapView(mapView: MapView?) {
        synchronized(lock) {
            this.mapView = mapView
        }
    }

    /**
     * Converts a screen coordinate (in pixels) to a coordinate within the map view's spatial reference.
     *
     * May return null in some circumstances, such as if the map view's spatial reference has not been determined yet.
     *
     * @param screenCoordinate the screen point, in pixels
     * @return a [Point] object, or null if the location could not be determined
     * @since 200.3.0
     */
    public fun screenToLocation(screenCoordinate: ScreenCoordinate): Point? {
        synchronized(lock) {
            return mapView?.screenToLocation(screenCoordinate)
        }
    }
}