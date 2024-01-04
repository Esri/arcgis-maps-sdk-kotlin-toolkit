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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.GraphicsOverlay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A collection class to encapsulate the [GraphicsOverlay] list used by the [com.arcgismaps.toolkit.geocompose.MapView]
 *
 * @since 200.4.0
 */
@Stable
public class GraphicsOverlayCollection : Iterable<GraphicsOverlay> {

    private val graphicsOverlays = mutableListOf<GraphicsOverlay>()

    private val _changed: MutableSharedFlow<ChangedEvent> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * [SharedFlow] used to emit changes made to the [graphicsOverlays] list
     */
    internal val changed: SharedFlow<ChangedEvent> = _changed.asSharedFlow()

    override fun iterator(): Iterator<GraphicsOverlay> {
        return graphicsOverlays.iterator()
    }

    /**
     * Add a [graphicsOverlay] to this GraphicsOverlayCollection.
     *
     * @return if the add operation succeeds, return true.
     * @since 200.4.0
     */
    public fun add(graphicsOverlay: GraphicsOverlay): Boolean {
        return if (graphicsOverlays.add(graphicsOverlay)) {
            _changed.tryEmit(ChangedEvent.Added(graphicsOverlay))
            true
        } else false
    }

    /**
     * Remove a [graphicsOverlay] from this GraphicsOverlayCollection.
     *
     * @return if the remove operation succeeds, return true.
     * @since 200.4.0
     */
    public fun remove(graphicsOverlay: GraphicsOverlay): Boolean {
        return if (graphicsOverlays.remove(graphicsOverlay)) {
            _changed.tryEmit(ChangedEvent.Removed(graphicsOverlay))
            true
        } else false
    }

    /**
     * Returns the number of graphics overlays in this GraphicsOverlayCollection.
     *
     * @since 200.4.0
     */
    public val size: Int
        get() = graphicsOverlays.size

    /**
     * Clears all graphics overlays from this GraphicsOverlayCollection.
     *
     * @since 200.4.0
     */
    public fun clear() {
        graphicsOverlays.clear()
        _changed.tryEmit(ChangedEvent.Cleared)
    }

    /**
     * Sealed class used to notify the compose MapView to update the GraphicOverlays on the
     * type of [ChangedEvent].
     *
     * @since 200.4.0
     */
    internal sealed class ChangedEvent() {
        class Added(val element: GraphicsOverlay) : ChangedEvent()
        class Removed(val element: GraphicsOverlay) : ChangedEvent()
        object Cleared : ChangedEvent()
    }
}


/**
 * Update the view-based [geoView]'s graphicsOverlays property to reflect changes made to the
 * [graphicsOverlayCollection] based on the type of [GraphicsOverlayCollection.ChangedEvent]
 *
 * @since 200.4.0
 */
@Composable
internal fun GraphicsOverlaysUpdater(
    graphicsOverlayCollection: GraphicsOverlayCollection,
    geoView: GeoView
) {
    LaunchedEffect(graphicsOverlayCollection) {
        // sync up the GeoView with the new graphics overlays
        geoView.graphicsOverlays.clear()
        graphicsOverlayCollection.forEach {
            geoView.graphicsOverlays.add(it)
        }
        // start observing graphicsOverlays for subsequent changes
        graphicsOverlayCollection.changed.collect { changedEvent ->
            when (changedEvent) {
                // On GraphicsOverlay added:
                is GraphicsOverlayCollection.ChangedEvent.Added ->
                    geoView.graphicsOverlays.add(changedEvent.element)

                // On GraphicsOverlay removed:
                is GraphicsOverlayCollection.ChangedEvent.Removed ->
                    geoView.graphicsOverlays.remove(changedEvent.element)

                // On GraphicsOverlays cleared:
                is GraphicsOverlayCollection.ChangedEvent.Cleared ->
                    geoView.graphicsOverlays.clear()
            }
        }
    }
}

/**
 * Create and [remember] a [GraphicsOverlayCollection].
 * [init] will be called when the [GraphicsOverlayCollection] is first created to configure its
 * initial state.
 *
 * @param key invalidates the remembered GraphicsOverlayCollection if different from the previous composition
 * @param init called when the [GraphicsOverlayCollection] is created to configure its initial state
 * @since 200.4.0
 */
@Composable
public inline fun rememberGraphicsOverlayCollection(
    key: Any? = null,
    crossinline init: GraphicsOverlayCollection.() -> Unit = {}
): GraphicsOverlayCollection = remember(key) {
    GraphicsOverlayCollection().apply(init)
}
