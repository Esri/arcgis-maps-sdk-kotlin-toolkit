package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
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

    public class SetViewpoint(public val viewpoint: Viewpoint): MapViewpointOperation()
    public class SetViewpointAnimated(
        public val viewpoint: Viewpoint,
        public val durationSeconds: Float = 1f, // TODO - determine default
        public val curve: AnimationCurve? = null
    ): MapViewpointOperation()

    public class SetViewpointCenter(
        public val center: Point,
        public val scale: Double? = null
    ): MapViewpointOperation()

    public class SetViewpointGeometry(
        public val boundingGeometry: Geometry,
        public val paddingInDips: Double? = null
    ): MapViewpointOperation()

    public class SetViewpointRotation(
        public val angleDegrees: Double
    ): MapViewpointOperation()

    public class SetViewpointScale(
        public val scale: Double
    ): MapViewpointOperation()
}


internal suspend fun MapViewpointOperation.SetViewpointAnimated.execute(mapView: MapView) =
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