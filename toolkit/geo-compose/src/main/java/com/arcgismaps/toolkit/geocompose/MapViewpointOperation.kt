/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Defines operations for setting the viewpoint of a composable [MapView].
 *
 * @since 200.3.0
 */
@Stable
public sealed class MapViewpointOperation {

    private val deferred = CompletableDeferred<Result<Boolean>>()

    /**
     * Awaits the completion of this MapViewpointOperation.
     *
     * @return a Result returning a boolean used to indicate if the operation completed successfully or not
     * @since 200.3.0
     */
    public suspend fun await(): Result<Boolean> = deferred.await()

    internal fun complete(result: Result<Boolean>) {
        deferred.complete(result)
    }

    /**
     * Changes the map view to the new viewpoint. The viewpoint is updated instantaneously.
     *
     * @property viewpoint the new viewpoint
     * @since 200.3.0
     */
    public class Set(public val viewpoint: Viewpoint) :
        MapViewpointOperation()

    /**
     * Animates the map view to the new viewpoint, taking the given number of seconds to complete the
     * navigation.
     *
     * @property viewpoint the new viewpoint
     * @property durationSeconds the duration of the animation in seconds
     * @property curve the animation curve to apply
     * @since 200.3.0
     */
    public class Animate(
        public val viewpoint: Viewpoint,
        public val durationSeconds: Float = 0.25f,
        public val curve: AnimationCurve? = null
    ) : MapViewpointOperation()

    /**
     * Animates the map view to the center point and scale.
     *
     * @property center the location on which the map should be centered
     * @property scale the new map scale
     * @since 200.3.0
     */
    public class Center(
        public val center: Point,
        public val scale: Double? = null
    ) : MapViewpointOperation()

    /**
     * Animates the map view to the bounding geometry with padding applied.
     *
     * @property boundingGeometry the geometry to zoom to. If the spatial reference of the geometry is
     * different to that of the composable [MapView], it will be reprojected appropriately
     * @property paddingInDips a distance around the geometry to include in the Viewpoint when zooming,
     * in density-independent pixels
     * @since 200.3.0
     */
    public class SetBoundingGeometry(
        public val boundingGeometry: Geometry,
        public val paddingInDips: Double? = null
    ) : MapViewpointOperation()

    /**
     * Animates the rotation of the map view to the provided angle.
     *
     * @property angleDegrees the new map rotation angle, in degrees counter-clockwise
     * @since 200.3.0
     */
    public class Rotate(
        public val angleDegrees: Double
    ) : MapViewpointOperation()

    /**
     * Animates the map view to zoom to a scale.
     *
     * @property scale the new map scale
     * @since 200.3.0
     */
    public class Scale(
        public val scale: Double
    ) : MapViewpointOperation()

    /**
     * Animates the map view's viewpoint to the viewpoint of the bookmark.
     *
     * @property bookmark bookmark to set
     * @since 200.3.0
     */
    public class SetBookmark(
        public val bookmark: Bookmark
    ) : MapViewpointOperation()
}

/**
 * Executes the [MapViewpointOperation] on the given view-based [MapView]. The operation can be awaited using
 * [MapViewpointOperation.await]. If the coroutine on which the viewpoint operation is executed is cancelled,
 * for example due to another viewpoint operation being executed, the [MapViewpointOperation.await] call
 * will return a Result with a boolean set to `false`, indicating that the viewpoint operation failed
 * to complete.
 *
 * @param mapView the view-based MapView to execute this operation on
 * @since 200.3.0
 */
internal suspend fun MapViewpointOperation.execute(mapView: com.arcgismaps.mapping.view.MapView) {
    when (this) {
        is MapViewpointOperation.Set -> {
            mapView.setViewpoint(this.viewpoint)
            this.complete(Result.success(true))
        }

        is MapViewpointOperation.Animate -> {
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

        is MapViewpointOperation.Center -> {
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

        is MapViewpointOperation.Rotate -> {
            try {
                val result = mapView.setViewpointRotation(this.angleDegrees)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }

        is MapViewpointOperation.Scale -> {
            try {
                val result = mapView.setViewpointScale(this.scale)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }

        is MapViewpointOperation.SetBookmark -> {
            try {
                val result = mapView.setBookmark(this.bookmark)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }

        is MapViewpointOperation.SetBoundingGeometry -> {
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
}
