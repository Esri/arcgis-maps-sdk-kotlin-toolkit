package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate


/**
 * Used to perform operations on a [com.arcgismaps.toolkit.geocompose.MapView].
 *
 * There should be a one-to-one relationship between a [MapViewOperator] and a composable [com.arcgismaps.toolkit.geocompose.MapView].
 * Operations can only be performed once the component has entered the composition.
 */
public class MapViewOperator() {

    /**
     * The [MapView] that this operator will operate on. This should be initialized by the [com.arcgismaps.toolkit.geocompose.MapView]
     * composable when it enters the composition and set to null when it is disposed by calling [setMapView].
     *
     * @since 200.3.0
     */
    private var mapView: MapView? = null

    /**
     * Sets the [mapView] parameter on this operator. This should be called by the [com.arcgismaps.toolkit.geocompose.MapView] composable
     * when it enters the composition and set to null when it is disposed by calling [setMapView].
     *
     * @since 200.3.0
     */
    internal fun setMapView(mapView: MapView?) {
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
     * @return A [ScreenCoordinate] for the screen in pixels. May return NAN for x and y, or null if an error occurs
     * @since 200.3.0
     */
    public fun locationToScreenOrNull(mapPoint: Point): ScreenCoordinate? {
        return try {
            // TODO: research when NAN can occur
            mapView?.locationToScreen(mapPoint)
        } catch (t: Throwable) {
            null
        }
    }
}