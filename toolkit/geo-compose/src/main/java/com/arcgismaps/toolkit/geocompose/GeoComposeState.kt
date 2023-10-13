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

import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import kotlinx.coroutines.channels.Channel

/**
 * Represents the state for the GeoCompose.
 *
 * @since 200.3.0
 */
public sealed class GeoComposeState {

    internal val viewpointChannel = Channel<ViewpointOperation>()
    internal val getCurrentViewpointChannel = Channel<GetCurrentViewpointOperation>()
    internal val setViewpointChannel = Channel<SetViewpointOperation>()

    /**
     * Change the GeoCompose to the new [viewpoint] with animation, using an optional
     * [durationSeconds] of seconds to complete the navigation.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        durationSeconds: Float? = null
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimated(
            viewpoint,
            durationSeconds
        ).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Retrieve the current viewpoint for this Map using the given [viewpointType].
     *
     * @since 200.3.0
     */
    public suspend fun getCurrentViewpoint(viewpointType: ViewpointType): Result<Viewpoint> =
        GetCurrentViewpointOperation.GetCurrentViewpoint(viewpointType).let {
            getCurrentViewpointChannel.send(it)
            it.await()
        }

    /**
     * Change the Map to the new [viewpoint]. The viewpoint is updated instantaneously.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpoint(viewpoint: Viewpoint): Result<Unit> =
        SetViewpointOperation.SetViewpoint(viewpoint).let {
            setViewpointChannel.send(it)
            it.await()
        }

}
