package com.arcgismaps.toolkit.ar.internal

import com.arcgismaps.mapping.view.CameraController
import com.google.ar.core.Frame

/**
 * Provides a common interface for classes that update the camera's position in world scale AR.
 *
 * @since 200.7.0
 */
internal interface WorldScaleCameraController {
    /**
     * The [CameraController] that will be passed to a scene view.
     *
     * @since 200.7.0
     */
    val cameraController: CameraController

    /**
     * Whether the origin camera has been set, used to determine if the an initial location has been
     * established.
     *
     * @since 200.7.0
     */
    val hasSetOriginCamera: Boolean

    /**
     * Updates the camera's position using the orientation of the [Frame.getCamera].
     *
     * @since 200.7.0
     */
    fun updateCamera(frame: Frame)
}