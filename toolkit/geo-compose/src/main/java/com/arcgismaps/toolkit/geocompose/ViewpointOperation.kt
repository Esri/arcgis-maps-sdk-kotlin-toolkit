package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve

public sealed class ViewpointOperation {
    public data class SetViewpoint(val viewpoint: Viewpoint): ViewpointOperation()

    public data class SetViewpointAnimated(
        val viewpoint: Viewpoint,
        val durationSeconds: Float = 1f, // TODO - determine default
        val curve: AnimationCurve? = null,
        val onCompleted: ((Boolean) -> Unit)? = null
    ): ViewpointOperation()

    public data class SetViewpointCenter(
        val center: Point,
        val scale: Double? = null,
        val onCompleted: ((Boolean) -> Unit)? = null
    ): ViewpointOperation()

    public data class SetViewpointGeometry(
        val boundingGeometry: Geometry,
        val paddingInDips: Double? = null,
        val onCompleted: ((Boolean) -> Unit)? = null
    ): ViewpointOperation()

    public data class SetViewpointRotation(
        val angleDegrees: Double,
        val onCompleted: ((Boolean) -> Unit)? = null
    ): ViewpointOperation()

    public data class SetViewpointScale(
        val scale: Double,
        val onCompleted: ((Boolean) -> Unit)? = null
    ): ViewpointOperation()
}