package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

@Stable
public sealed class MapViewpointOperation {

    private val deferred = CompletableDeferred<Result<Boolean>>()
    public suspend fun await(): Result<Boolean> = deferred.await()

    internal fun complete(result: Result<Boolean>) {
        deferred.complete(result)
    }

    public class Set(public val viewpoint: com.arcgismaps.mapping.Viewpoint): MapViewpointOperation()
    public class Animate(
        public val viewpoint: com.arcgismaps.mapping.Viewpoint,
        public val durationSeconds: Float = 1f, // TODO - determine default
        public val curve: AnimationCurve? = null
    ): MapViewpointOperation()

    public class Center(
        public val center: Point,
        public val scale: Double? = null
    ): MapViewpointOperation()

    public class Bounding(
        public val boundingGeometry: Geometry,
        public val paddingInDips: Double? = null
    ): MapViewpointOperation()

    public class Rotate(
        public val angleDegrees: Double
    ): MapViewpointOperation()

    public class Scale(
        public val scale: Double
    ): MapViewpointOperation()
}


internal suspend fun MapViewpointOperation.Animate.execute(mapView: MapView) =
    try {
        val result = if (this.curve != null) {
            mapView.setViewpointAnimated(
                this.viewpoint,
                this.durationSeconds,
                this.curve
            )
        } else {
            mapView.setViewpointAnimated(
                this.viewpoint,
                this.durationSeconds
            )
        }
        this.complete(result)
    } catch (e: CancellationException) {
        this.complete(Result.success(false))
        throw e
    }