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
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.SceneView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A collection class to encapsulate the [AnalysisOverlay] list used by the [com.arcgismaps.toolkit.geocompose.SceneView]
 *
 * @since 200.4.0
 */
@Stable
public class AnalysisOverlayCollection : Iterable<AnalysisOverlay> {

    private val analysisOverlays = mutableListOf<AnalysisOverlay>()

    private val _changed: MutableSharedFlow<ChangedEvent> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * [SharedFlow] used to emit changes made to the [analysisOverlays] list
     */
    internal val changed: SharedFlow<ChangedEvent> = _changed.asSharedFlow()

    override fun iterator(): Iterator<AnalysisOverlay> {
        return analysisOverlays.iterator()
    }

    /**
     * Add a [analysisOverlay] to this AnalysisOverlayCollection.
     *
     * @return if the add operation succeeds, return true.
     * @since 200.4.0
     */
    public fun add(analysisOverlay: AnalysisOverlay): Boolean {
        return if (analysisOverlays.add(analysisOverlay)) {
            _changed.tryEmit(ChangedEvent.Added(analysisOverlay))
            true
        } else false
    }

    /**
     * Remove a [analysisOverlay] from this AnalysisOverlayCollection.
     *
     * @return if the remove operation succeeds, return true.
     * @since 200.4.0
     */
    public fun remove(analysisOverlay: AnalysisOverlay): Boolean {
        return if (analysisOverlays.remove(analysisOverlay)) {
            _changed.tryEmit(ChangedEvent.Removed(analysisOverlay))
            true
        } else false
    }

    /**
     * Returns the number of analysis overlays in this AnalysisOverlayCollection.
     *
     * @since 200.4.0
     */
    public val size: Int
        get() = analysisOverlays.size

    /**
     * Clears all analysis overlays from this AnalysisOverlayCollection.
     *
     * @since 200.4.0
     */
    public fun clear() {
        analysisOverlays.clear()
        _changed.tryEmit(ChangedEvent.Cleared)
    }

    /**
     * Sealed class used to notify the compose SceneView to update the AnalysisOverlays on the
     * type of [ChangedEvent].
     *
     * @since 200.4.0
     */
    internal sealed class ChangedEvent() {
        class Added(val element: AnalysisOverlay) : ChangedEvent()
        class Removed(val element: AnalysisOverlay) : ChangedEvent()
        object Cleared : ChangedEvent()
    }
}


/**
 * Update the view-based [SceneView]'s analysisOverlays property to reflect changes made to the
 * [analysisOverlayCollection] based on the type of [AnalysisOverlayCollection.ChangedEvent]
 *
 * @since 200.4.0
 */
@Composable
internal fun AnalysisOverlaysUpdater(
    analysisOverlayCollection: AnalysisOverlayCollection,
    sceneView: SceneView
) {
    LaunchedEffect(analysisOverlayCollection) {
        // sync up the GeoView with the new graphics overlays
        sceneView.analysisOverlays.clear()
        analysisOverlayCollection.forEach {
            sceneView.analysisOverlays.add(it)
        }
        // start observing analysisOverlays for subsequent changes
        analysisOverlayCollection.changed.collect { changedEvent ->
            when (changedEvent) {
                // On AnalysisOverlay added:
                is AnalysisOverlayCollection.ChangedEvent.Added ->
                    sceneView.analysisOverlays.add(changedEvent.element)

                // On AnalysisOverlay removed:
                is AnalysisOverlayCollection.ChangedEvent.Removed ->
                    sceneView.analysisOverlays.remove(changedEvent.element)

                // On AnalysisOverlays cleared:
                is AnalysisOverlayCollection.ChangedEvent.Cleared ->
                    sceneView.analysisOverlays.clear()
            }
        }
    }
}
