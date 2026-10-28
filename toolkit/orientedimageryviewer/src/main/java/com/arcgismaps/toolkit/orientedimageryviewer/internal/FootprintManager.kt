/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import android.util.Log
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Manages the visibility and updates of oriented imagery footprints and camera locations.
 *
 * @param layer The oriented imagery layer to manage footprints for.
 * @param overlay The graphics overlay to manage camera location graphics for.
 * @param scope The coroutine scope for asynchronous updates.
 * @param activeImage A function that returns the currently active oriented image, or null if none.
 * @param additionalImages A function that returns a list of additional oriented images to display footprints for.
 * @param inputLocation A function that returns the current input location as a point, or null if none.
 * @param uiState A function that returns the current UI state of the oriented imagery viewer.
 * @param createFootprint A function that creates an oriented image footprint from an oriented image. Defaults to [OrientedImageFootprint].
 */
internal class FootprintManager(
    private val layer: OrientedImageryLayer,
    private val overlay: GraphicsOverlay,
    private val scope: CoroutineScope,
    private val activeImage: () -> OrientedImage?,
    private val additionalImages: () -> List<OrientedImage>,
    private val inputLocation: () -> Point?,
    private val uiState: () -> OrientedImageryViewerUiState,
    private val createFootprint: (OrientedImage) -> OrientedImageFootprint = ::OrientedImageFootprint
) {
    private var active: OrientedImageFootprint? = null
    private var activeCamera: Graphic? = null
    private var inputGraphic: Graphic? = null
    private var additionalFootprints: List<OrientedImageFootprint> = emptyList()
    private var additionalCameras: List<Graphic> = emptyList()
    private var updateJob: Job? = null
    private var pendingUpdate: (suspend (OrientedImageFootprint) -> Result<*>)? = null

    fun setActiveVisible(visible: Boolean) {
        if (visible) {
            val image = activeImage() ?: return
            val footprint = active ?: createFootprint(image).also {
                it.outlineColor = Color.transparent
                active = it
                activeCamera = Graphic(image.geometry, ImageDefaults.activeCameraLocationSymbol()).also(overlay.graphics::add)
            }
            if (footprint !in layer.visibleFootprints) layer.visibleFootprints.add(footprint)
        } else {
            active?.let { layer.visibleFootprints.remove(it) }
            cancelUpdates()
        }
        activeCamera?.isVisible = visible
        syncInputGraphic(visible)
    }

    private fun syncInputGraphic(visible: Boolean) {
        inputGraphic?.let { overlay.graphics.remove(it) }
        inputGraphic = null
        if (visible && !uiState().isSequentialNavigationEnabled && activeImage() != null) {
            inputLocation()?.let { point ->
                inputGraphic = Graphic(point, ImageDefaults.inputLocationSymbol()).also(overlay.graphics::add)
            }
        }
    }

    fun setAdditionalFootprintsVisible(visible: Boolean) {
        if (!visible) {
            layer.visibleFootprints.removeAll(additionalFootprints)
            return
        }
        if (additionalFootprints.isEmpty()) {
            additionalFootprints = additionalImages().map { image ->
                createFootprint(image).apply {
                    fillColor = ImageDefaults.ADDITIONAL_FOOTPRINT_COLOR
                    outlineColor = Color.transparent
                }
            }
        }
        layer.visibleFootprints.addAll(additionalFootprints.filterNot { it in layer.visibleFootprints })
    }

    fun setAdditionalCamerasVisible(visible: Boolean) {
        if (!visible) {
            overlay.graphics.removeAll(additionalCameras)
            return
        }
        if (additionalCameras.isEmpty()) {
            additionalCameras = additionalImages().map { image ->
                Graphic(image.geometry, ImageDefaults.additionalCameraLocationSymbol())
            }
        }
        overlay.graphics.addAll(additionalCameras.filterNot { it in overlay.graphics })
    }

    fun update(update: suspend (OrientedImageFootprint) -> Result<*>) {
        if (!uiState().isActiveFootprintVisible) return
        val footprint = active ?: return
        pendingUpdate = update
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            while (true) {
                val next = pendingUpdate ?: break
                pendingUpdate = null
                try {
                    next(footprint).getOrThrow()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    currentCoroutineContext().ensureActive()
                    Log.w("OrientedImagery", "Unable to update the active footprint", error)
                }
                currentCoroutineContext().ensureActive()
                if (active !== footprint) break
            }
        }
    }

    fun imageChanged() {
        reset()
        val state = uiState()
        setActiveVisible(state.isActiveFootprintVisible)
        setAdditionalFootprintsVisible(state.isAdditionalFootprintsVisible)
        setAdditionalCamerasVisible(state.isAdditionalCameraLocationsVisible)
    }

    fun navigationModeChanged() {
        syncInputGraphic(uiState().isActiveFootprintVisible)
    }

    fun reset() {
        cancelUpdates()
        active?.let { layer.visibleFootprints.remove(it) }
        layer.visibleFootprints.removeAll(additionalFootprints)
        activeCamera?.let { overlay.graphics.remove(it) }
        inputGraphic?.let { overlay.graphics.remove(it) }
        overlay.graphics.removeAll(additionalCameras)
        active = null
        activeCamera = null
        inputGraphic = null
        additionalFootprints = emptyList()
        additionalCameras = emptyList()
    }

    private fun cancelUpdates() {
        updateJob?.cancel()
        updateJob = null
        pendingUpdate = null
    }
}
