package com.arcgismaps.toolkit.ar

import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.ar.internal.FrameDerivatives

public class ArOrientedImage internal constructor(private val frame: FrameDerivatives) {
    public val location: Point by lazy {
        frame.projectedLocation
    }
}