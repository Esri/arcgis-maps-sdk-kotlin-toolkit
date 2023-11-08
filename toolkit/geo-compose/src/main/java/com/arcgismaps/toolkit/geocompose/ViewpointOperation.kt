package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.CancellationException

@Stable
public sealed class MapViewpointOperation {
    public class SetViewpoint(public val viewpoint: Viewpoint): MapViewpointOperation()
    public class SetViewpointAnimated(
        public val viewpoint: Viewpoint,
        public val durationSeconds: Float = 1f, // TODO - determine default
        public val curve: AnimationCurve? = null,
        public val onCompleted: ((Boolean) -> Unit)? = null
    ): MapViewpointOperation()

    public class SetViewpointCenter(
        public val center: Point,
        public val scale: Double? = null,
        public val onCompleted: ((Boolean) -> Unit)? = null
    ): MapViewpointOperation()

    public class SetViewpointGeometry(
        public val boundingGeometry: Geometry,
        public val paddingInDips: Double? = null,
        public val onCompleted: ((Boolean) -> Unit)? = null
    ): MapViewpointOperation()

    public class SetViewpointRotation(
        public val angleDegrees: Double,
        public val onCompleted: ((Boolean) -> Unit)? = null
    ): MapViewpointOperation()

    public class SetViewpointScale(
        public val scale: Double,
        public val onCompleted: ((Boolean) -> Unit)? = null
    ): MapViewpointOperation()
}


internal suspend fun MapViewpointOperation.SetViewpointAnimated.execute(mapView: MapView) =
    try {
        if (this.curve != null) {
            mapView.setViewpointAnimated(
                this.viewpoint,
                this.durationSeconds,
                this.curve
            ).run {
                onSuccess {
                    onCompleted?.invoke(it)
                }.onFailure {
                    onCompleted?.invoke(false)
                }
            }
        } else {
            mapView.setViewpointAnimated(
                this.viewpoint,
                this.durationSeconds
            ).run {
                onSuccess {
                    onCompleted?.invoke(it)
                }.onFailure {
                    onCompleted?.invoke(false)
                }
            }
        }
    } catch (e: CancellationException) {
        onCompleted?.invoke(false)
        throw e
    }