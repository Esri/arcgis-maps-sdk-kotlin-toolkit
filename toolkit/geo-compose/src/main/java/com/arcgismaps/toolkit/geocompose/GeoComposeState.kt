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

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Represents the state for the GeoCompose.
 *
 * @since 200.3.0
 */
public sealed class GeoComposeState {

    /**
     * A [MutableSharedFlow] backing the public immutable viewpointChanged [SharedFlow].
     *
     * @since 200.3.0
     */
    private val _viewpointChanged: MutableSharedFlow<Unit> =
        MutableSharedFlow(
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    /**
     * A [SharedFlow] which notifies when the viewpoint of GeoCompose has changed.
     *
     * @since 200.3.0
     */
    public val viewpointChanged: SharedFlow<Unit> = _viewpointChanged.asSharedFlow()

    /**
     * Emits the viewpointChanged event.
     *
     * @since 200.3.0
     */
    internal fun notifyViewpointChanged() {
        _viewpointChanged.tryEmit(Unit)
    }
}
