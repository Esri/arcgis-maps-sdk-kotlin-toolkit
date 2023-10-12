/*
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

import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the state for the Map.
 *
 * @since 200.3.0
 */
public class MapState(arcGISMap: ArcGISMap? = null) : GeoComposeState() {
    private val _arcGISMap: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
    public val arcGISMap: StateFlow<ArcGISMap?> = _arcGISMap.asStateFlow()

    public fun setArcGISMap(arcGISMap: ArcGISMap) {
        _arcGISMap.value = arcGISMap
    }

    init {
        _arcGISMap.value = arcGISMap
    }

    /**
     * Change the Map to the [viewpoint] with [durationSeconds] and animation [curve] asynchronously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        durationSeconds: Float,
        curve: AnimationCurve
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimated(viewpoint, durationSeconds, curve)
            .let {
                viewpointChannel.send(it)
                it.await()
            }

    /**
     * Change the Map to the [center] point using an optional [scale] asynchronously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointCenter(center: Point, scale: Double? = null): Result<Boolean> =
        if (scale != null) ViewpointOperation.ViewpointCenter(center, scale).let {
            viewpointChannel.send(it)
            it.await()
        } else ViewpointOperation.ViewpointCenter(center).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Change the Map to the [boundingGeometry] using an optional [paddingInDips] asynchronously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointGeometry(
        boundingGeometry: Geometry,
        paddingInDips: Double? = null
    ): Result<Boolean> =
        if (paddingInDips != null) ViewpointOperation.ViewpointGeometry(
            boundingGeometry,
            paddingInDips
        ).let {
            viewpointChannel.send(it)
            it.await()
        } else ViewpointOperation.ViewpointGeometry(boundingGeometry).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Rotates the Map to the provided [angleDegrees] asynchronously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointRotation(angleDegrees: Double): Result<Boolean> =
        ViewpointOperation.ViewpointRotation(angleDegrees).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Change the Map to zoom to a [scale] asynchronously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointScale(scale: Double): Result<Boolean> =
        ViewpointOperation.ViewpointScale(scale).let {
            viewpointChannel.send(it)
            it.await()
        }
}
