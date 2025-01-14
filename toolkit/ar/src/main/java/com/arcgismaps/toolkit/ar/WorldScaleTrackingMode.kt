package com.arcgismaps.toolkit.ar

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.location.LocationDataSource
import com.arcgismaps.location.SystemLocationDataSource

public sealed class WorldScaleTrackingMode {

    /**
     * The device's position and orientation are tracked using the provided [LocationDataSource].
     *
     * @since 200.7.0
     */
    public class World(locationDataSource: LocationDataSource) : WorldScaleTrackingMode() {
        public val locationDataSource: LocationDataSource = locationDataSource

        /**
         * Constructs a new instance of this class with the default [SystemLocationDataSource].
         *
         * @throws IllegalStateException if [ArcGISEnvironment.applicationContext] is not set
         * @since 200.7.0
         */
        public constructor() : this(SystemLocationDataSource())
    }

    /**
     * The device's position and orientation are tracked using the [ARCore Geospatial API](https://developers.google.com/ar/develop/geospatial)
     *
     * @since 200.7.0
     */
    public data object Geospatial: WorldScaleTrackingMode()

    /**
     * The [WorldScaleSceneView] will attempt to track the device's position and orientation using
     * the [ARCore Geospatial API](https://developers.google.com/ar/develop/geospatial). If this is not
     * available on the device, it will fall back to using the provided [LocationDataSource].
     *
     * @see [WorldScaleTrackingMode.Geospatial]
     * @since 200.7.0
     */
    public class PreferGeospatial(locationDataSource: LocationDataSource) : WorldScaleTrackingMode() {
        public val locationDataSource: LocationDataSource = locationDataSource

        /**
         * Constructs a new instance of this class with the default [SystemLocationDataSource].
         *
         * @throws IllegalStateException if [ArcGISEnvironment.applicationContext] is not set
         * @since 200.7.0
         */
        public constructor() : this(SystemLocationDataSource())
    }
}