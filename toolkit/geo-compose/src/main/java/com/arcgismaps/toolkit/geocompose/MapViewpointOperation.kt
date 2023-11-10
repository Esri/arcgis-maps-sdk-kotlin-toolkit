package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Bookmark
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

    internal abstract suspend fun execute(mapView: MapView)

    public class Set(public val viewpoint: com.arcgismaps.mapping.Viewpoint): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            mapView.setViewpoint(this.viewpoint)
            this.complete(Result.success(true))
        }
    }

    public class Animate(
        public val viewpoint: com.arcgismaps.mapping.Viewpoint,
        public val durationSeconds: Float = 1f, // TODO - determine default
        public val curve: AnimationCurve? = null
    ): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
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
        }
    }

    public class Center(
        public val center: Point,
        public val scale: Double? = null
    ): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            try {
                val result = if (this.scale != null) {
                    mapView.setViewpointCenter(this.center, this.scale)
                } else {
                    mapView.setViewpointCenter(this.center)
                }
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
    }

    public class SetBoundingGeometry(
        public val boundingGeometry: Geometry,
        public val paddingInDips: Double? = null
    ): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            try {
                val result = if (this.paddingInDips != null) {
                    mapView.setViewpointGeometry(this.boundingGeometry, this.paddingInDips)
                } else {
                    mapView.setViewpointGeometry(this.boundingGeometry)
                }
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
    }

    public class Rotate(
        public val angleDegrees: Double
    ): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            try {
                val result = mapView.setViewpointRotation(this.angleDegrees)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
    }

    public class Scale(
        public val scale: Double
    ): MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            try {
                val result = mapView.setViewpointScale(this.scale)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
    }

    public class SetBookmark(
        public val bookmark: Bookmark
    ) : MapViewpointOperation() {
        override suspend fun execute(mapView: MapView) {
            try {
                val result = mapView.setBookmark(this.bookmark)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
    }
}
