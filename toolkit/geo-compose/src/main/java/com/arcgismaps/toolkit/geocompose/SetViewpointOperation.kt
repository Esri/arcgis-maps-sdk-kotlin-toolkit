package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.Viewpoint

/**
 * Represents the [SetViewpoint] message to be sent to the channel.
 * Change the Map to the new viewpoint. The viewpoint is updated instantaneously.
 *
 * @since 200.3.0
 */
internal sealed class SetViewpointOperation {
    data class SetViewpoint(val viewpoint: Viewpoint) : SetViewpointOperation()
}
