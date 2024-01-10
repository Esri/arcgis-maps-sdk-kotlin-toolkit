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
import com.arcgismaps.mapping.view.ImageOverlay
import com.arcgismaps.mapping.view.SceneView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A collection class to encapsulate the [ImageOverlay] list used by the [com.arcgismaps.toolkit.geocompose.SceneView]
 *
 * @since 200.4.0
 */
@Stable
public class ImageOverlayCollection : Iterable<ImageOverlay> {

    private val imageOverlays = mutableListOf<ImageOverlay>()

    private val _changed: MutableSharedFlow<ChangedEvent> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * [SharedFlow] used to emit changes made to the [imageOverlays] list
     */
    internal val changed: SharedFlow<ChangedEvent> = _changed.asSharedFlow()

    override fun iterator(): Iterator<ImageOverlay> {
        return imageOverlays.iterator()
    }

    /**
     * Add a [imageOverlay] to this ImageOverlayCollection.
     *
     * @return if the add operation succeeds, return true.
     * @since 200.4.0
     */
    public fun add(imageOverlay: ImageOverlay): Boolean {
        return if (imageOverlays.add(imageOverlay)) {
            _changed.tryEmit(ChangedEvent.Added(imageOverlay))
            true
        } else false
    }

    /**
     * Remove a [imageOverlay] from this ImageOverlayCollection.
     *
     * @return if the remove operation succeeds, return true.
     * @since 200.4.0
     */
    public fun remove(imageOverlay: ImageOverlay): Boolean {
        return if (imageOverlays.remove(imageOverlay)) {
            _changed.tryEmit(ChangedEvent.Removed(imageOverlay))
            true
        } else false
    }

    /**
     * Returns the number of graphics overlays in this ImageOverlayCollection.
     *
     * @since 200.4.0
     */
    public val size: Int
        get() = imageOverlays.size

    /**
     * Clears all graphics overlays from this ImageOverlayCollection.
     *
     * @since 200.4.0
     */
    public fun clear() {
        imageOverlays.clear()
        _changed.tryEmit(ChangedEvent.Cleared)
    }

    /**
     * Sealed class used to notify the compose SceneView to update the ImageOverlays on the
     * type of [ChangedEvent].
     *
     * @since 200.4.0
     */
    internal sealed class ChangedEvent() {
        class Added(val element: ImageOverlay) : ChangedEvent()
        class Removed(val element: ImageOverlay) : ChangedEvent()
        object Cleared : ChangedEvent()
    }
}
