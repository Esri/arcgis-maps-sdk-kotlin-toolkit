package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

private const val MAPVIEW_NULL_MESSAGE: String = "This operation cannot be performed if a MapView composable which uses this MapViewOperator is not part of the composition."

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

    private val lock = Unit

    /**
     * Sets the [mapView] parameter on this operator. This should be called by the [com.arcgismaps.toolkit.geocompose.MapView] composable
     * when it enters the composition and set to null when it is disposed by calling [setMapView].
     *
     * @since 200.3.0
     */
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
     * @throws IllegalStateException if a [com.arcgismaps.toolkit.geocompose.MapView] composable which uses this [MapViewOperator] is not currently part of the composition.
     * @since 200.3.0
     */
    public fun screenToLocation(screenCoordinate: ScreenCoordinate): Point? {
        synchronized(lock) {
            check(mapView != null) {
                MAPVIEW_NULL_MESSAGE
            }
            return mapView!!.screenToLocation(screenCoordinate)
        }
    }

    /**
     * Converts a coordinate within the map view's spatial reference to a screen coordinate (in pixels).
     * If the wraparound mode is active, this method returns the closest screen location matching the
     * specified map location. 'Closest' meaning: If it's in view, return that location, otherwise return
     * for the frame where the location is the closest to the center of the view.
     *
     * @param mapPoint a [Point] object representing a coordinate on the map
     * @return A [ScreenCoordinate] for the screen in pixels. NAN for x and y if an error occurs
     * @throws IllegalStateException if a [MapView] composable which uses this [MapViewOperator] is not currently part of the composition.
     * @since 200.3.0
     */
    public fun locationToScreen(mapPoint: Point): ScreenCoordinate {
        synchronized(lock) {
            check(mapView != null) {
                MAPVIEW_NULL_MESSAGE
            }
            return mapView!!.locationToScreen(mapPoint)
        }
    }
}