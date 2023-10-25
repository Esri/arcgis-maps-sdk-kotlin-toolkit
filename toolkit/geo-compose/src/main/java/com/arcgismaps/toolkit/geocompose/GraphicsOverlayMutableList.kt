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

import com.arcgismaps.mapping.view.GraphicsOverlay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A collection class to encapsulate the [GraphicsOverlay] list used by the [com.arcgismaps.toolkit.geocompose.MapView]
 * @since 200.3.0
 */
public class GraphicsOverlayMutableList :
    AbstractMutableList<GraphicsOverlay>() {

    private val graphicsOverlays = mutableListOf<GraphicsOverlay>()

    private val _changed: MutableSharedFlow<ChangedEvent> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * [SharedFlow] used to emit changes made to the [graphicsOverlays] list
     */
    internal val changed: SharedFlow<ChangedEvent> = _changed.asSharedFlow()

    override fun add(index: Int, element: GraphicsOverlay) {
        graphicsOverlays.add(element)
        _changed.tryEmit(ChangedEvent.Added(element))
    }

    override fun add(element: GraphicsOverlay): Boolean {
        val boolean = graphicsOverlays.add(element)
        _changed.tryEmit(ChangedEvent.Added(element))
        return boolean
    }

    override fun get(index: Int): GraphicsOverlay {
        return graphicsOverlays[index]
    }

    override fun set(index: Int, element: GraphicsOverlay): GraphicsOverlay {
        graphicsOverlays[index] = element
        _changed.tryEmit(ChangedEvent.Added(element))
        return element
    }

    override fun removeAt(index: Int): GraphicsOverlay {
        val element = graphicsOverlays.removeAt(index)
        _changed.tryEmit(ChangedEvent.Removed(element))
        return element
    }

    override val size: Int
        get() = graphicsOverlays.size

    override fun clear() {
        graphicsOverlays.clear()
        _changed.tryEmit(ChangedEvent.Cleared())
    }

    /**
     * Sealed class used to notify the compose MapView to update the GraphicOverlays on the
     * type of [ChangedEvent].
     * @since 200.3.0
     */
    internal sealed class ChangedEvent(internal val element: GraphicsOverlay? = null) {
        class Added(element: GraphicsOverlay) : ChangedEvent(element)
        class Removed(element: GraphicsOverlay) : ChangedEvent(element)
        class Cleared : ChangedEvent()
    }
}

