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
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.AnimationCurve
import kotlinx.coroutines.channels.Channel
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

    internal val currentViewpointType: MutableStateFlow<ViewpointType?> = MutableStateFlow(null)
    internal val viewpointChannel = Channel<ViewpointOperation>()
    internal val viewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)

    public fun setArcGISMap(arcGISMap: ArcGISMap) {
        _arcGISMap.value = arcGISMap
    }

    init {
        _arcGISMap.value = arcGISMap
    }

    public fun getCurrentViewpoint(viewpointType: ViewpointType): StateFlow<Viewpoint?> {
        currentViewpointType.value = viewpointType
        return viewpoint
    }

    public fun setViewpoint(viewpoint: Viewpoint) {
        this.viewpoint.value = viewpoint
    }

    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimated(viewpoint).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        durationSeconds: Float
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimatedWithDuration(viewpoint, durationSeconds).let {
            viewpointChannel.send(it)
            it.await()
        }


    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        durationSeconds: Float,
        curve: AnimationCurve
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimatedWithDurationAndCurve(viewpoint, durationSeconds, curve)
            .let {
                viewpointChannel.send(it)
                it.await()
            }

    public suspend fun setViewpointCenter(point: Point): Result<Boolean> =
        ViewpointOperation.ViewpointCenter(point).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointCenter(center: Point, scale: Double): Result<Boolean> =
        ViewpointOperation.ViewpointCenterAndScale(center, scale).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointGeometry(boundingGeometry: Geometry): Result<Boolean> =
        ViewpointOperation.ViewpointGeometry(boundingGeometry).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointGeometry(
        boundingGeometry: Geometry,
        paddingInDips: Double
    ): Result<Boolean> =
        ViewpointOperation.ViewpointGeometryAndPadding(boundingGeometry, paddingInDips).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointRotation(angleDegrees: Double): Result<Boolean> =
        ViewpointOperation.ViewpointRotation(angleDegrees).let {
            viewpointChannel.send(it)
            it.await()
        }

    public suspend fun setViewpointScale(scale: Double): Result<Boolean> =
        ViewpointOperation.ViewpointScale(scale).let {
            viewpointChannel.send(it)
            it.await()
        }
}
