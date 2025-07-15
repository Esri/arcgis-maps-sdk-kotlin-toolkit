package com.arcgismaps.toolkit.ar

/**
 * Represents the availability of Google's
 * [Virtual Positioning Service](https://developers.google.com/ar/develop/java/geospatial/check-vps-availability)
 * at a specific location.
 *
 * @see WorldScaleSceneViewProxy.checkVpsAvailability
 * @since 200.8.0
 */
public sealed class WorldScaleVpsAvailability {
    /**
     * VPS is available at the requested location.
     *
     * @since 200.8.0
     */
    public data object Available : WorldScaleVpsAvailability()

    /**
     * An authorization error occurred when communicating with the Google Cloud ARCore API. See
     * [Enable the Geospatial API](https://developers.google.com/ar/develop/java/geospatial/enable)
     * for troubleshooting steps.
     *
     * @since 200.8.0
     */
    public data object NotAuthorized : WorldScaleVpsAvailability()

    /**
     * Too many requests were made to the ARCore Service, the usage quota has been exceeded.
     * See [Geospatial API usage quota]()https://developers.google.com/ar/develop/java/geospatial/api-usage-quota)
     * for more information.
     *
     * @since 200.8.0
     */
    public data object ResourceExhausted : WorldScaleVpsAvailability()

    /**
     * VPS is not available at the requested location.
     *
     * @since 200.8.0
     */
    public data object Unavailable : WorldScaleVpsAvailability()
}
