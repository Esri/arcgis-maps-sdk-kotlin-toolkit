package com.arcgismaps.toolkit.ar.internal

import com.arcgismaps.mapping.view.CameraController
import com.google.ar.core.Frame

internal interface WorldScaleCameraController {
    val cameraController: CameraController
    val hasSetOriginCamera: Boolean
    fun updateCamera(frame: Frame)
}