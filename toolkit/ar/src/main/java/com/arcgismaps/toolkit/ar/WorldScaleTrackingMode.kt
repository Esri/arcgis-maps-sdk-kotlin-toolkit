package com.arcgismaps.toolkit.ar

public sealed class WorldScaleTrackingMode private constructor() {
    /**
     * The [WorldScaleSceneView] is tracking the user's position and orientation in the real world
     * using GPS positioning in combination with [ARCore](https://developers.google.com/ar/develop).
     *
     * @since 200.7.0
     */
    public data object WorldTracking : WorldScaleTrackingMode()

    /**
     * The [WorldScaleSceneView] is tracking the user's position and orientation in the real world
     * using [ARCore's Geospatial API](https://developers.google.com/ar/develop/geospatial). If
     * available, this will make use of [VPS](https://developers.google.com/ar/develop/java/geospatial/check-vps-availability)
     * for more accurate positioning.
     *
     * @since 200.7.0
     */
    public data object GeoTracking : WorldScaleTrackingMode()
}
