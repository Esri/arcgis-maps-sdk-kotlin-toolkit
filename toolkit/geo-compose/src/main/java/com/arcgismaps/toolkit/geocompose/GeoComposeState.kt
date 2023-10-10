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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the state for the GeoCompose.
 *
 * @since 200.3.0
 */
public sealed class GeoComposeState {

    private val currentViewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    internal val currentViewpointType: MutableStateFlow<ViewpointType?> = MutableStateFlow(null)
    internal val viewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    internal val viewpointChannel = Channel<ViewpointOperation>()

    /**
     * Change the GeoCompose to the new [viewpoint] with animation.
     * This function uses the standard animation duration.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimated(viewpoint).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Change the GeoCompose to the new [viewpoint] with animation, taking the given number
     * of seconds to complete the navigation.
     *
     * @since 200.3.0
     */
    public suspend fun setViewpointAnimated(
        viewpoint: Viewpoint,
        durationSeconds: Float
    ): Result<Boolean> =
        ViewpointOperation.ViewpointAnimatedWithDuration(viewpoint, durationSeconds).let {
            viewpointChannel.send(it)
            it.await()
        }

    /**
     * Set the [viewpoint] and is updated instantaneously.
     *
     * @since 200.3.0
     */
    public fun setViewpoint(viewpoint: Viewpoint) {
        this.viewpoint.value = viewpoint
    }

    /**
     * Returns the StateFlow of the [currentViewpoint] based on the [viewpointType]
     * which emits on every viewpoint change.
     *
     * @since 200.3.0
     */
    public fun getCurrentViewpoint(viewpointType: ViewpointType): StateFlow<Viewpoint?> {
        currentViewpointType.value = viewpointType
        return currentViewpoint
    }

    /**
     * Sets the [currentViewpoint] to the [viewpoint] based on the currently set [ViewpointType].
     *
     * @since 200.3.0
     */
    internal fun setCurrentViewpoint(viewpoint: Viewpoint) {
        currentViewpoint.value = viewpoint
    }
}
