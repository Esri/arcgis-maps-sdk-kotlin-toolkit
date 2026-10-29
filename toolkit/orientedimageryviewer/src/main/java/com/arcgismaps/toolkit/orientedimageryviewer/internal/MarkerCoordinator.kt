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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerOptionId
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerSymbols
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class PanoramicMarker(
    val imagePoint: DoubleXY,
    val isFromGeoView: Boolean,
    val symbol: SimpleMarkerSymbol
)

/**
 * Coordinates the addition of markers to the map and to the active image. The active image may be
 * a raster or a panorama, and the marker may be added from either the map or the image. The marker
 * is only added to the image if it is still active and visible at the time of addition.
 *
 * @param layer The oriented imagery layer to which the markers belong.
 * @param mapOverlay The graphics overlay for the map view.
 * @param activeImage A function that returns the currently active oriented image, or null if none.
 * @param uiState A function that returns the current UI state of the oriented imagery viewer.
 */
internal class MarkerCoordinator(
    private val layer: OrientedImageryLayer,
    private val mapOverlay: GraphicsOverlay,
    private val activeImage: () -> OrientedImage?,
    private val uiState: () -> OrientedImageryViewerUiState
) {
    private class Target(
        val revision: Long,
        val owner: Any,
        val prepare: (DoubleXY, SimpleMarkerSymbol, Boolean) -> (() -> Unit)?
    )

    private data class Operation(
        val image: OrientedImage,
        val revision: Long,
        val generation: Long,
        val target: Target,
        val symbol: SimpleMarkerSymbol
    )

    private data class Selection(val id: MarkerOptionId?, val symbol: SimpleMarkerSymbol)

    private var selection by mutableStateOf(Selection(null, ImageDefaults.defaultMarkerSymbol()))
    val currentSymbol: SimpleMarkerSymbol get() = selection.symbol
    val selectedOptionId: MarkerOptionId? get() = selection.id
    var isSelectionVisible by mutableStateOf(false)
    var panoramicMarkers: List<PanoramicMarker> by mutableStateOf(emptyList())
        private set
    private var generation = 0L
    private var target: Target? = null
    private var rasterOverlay: GraphicsOverlay? = null
    private var imageGraphics: List<Graphic> = emptyList()
    private var mapGraphics: List<Graphic> = emptyList()

    fun selectOption(id: MarkerOptionId) {
        selection = Selection(id, MarkerSymbols.options.first { it.id == id }.symbol.clone())
        isSelectionVisible = false
    }

    fun selectSymbol(symbol: SimpleMarkerSymbol) {
        val id = MarkerSymbols.options.firstOrNull { it.symbol === symbol }?.id
        selection = Selection(id, symbol.clone())
        isSelectionVisible = false
    }

    fun invalidateOperations() { ++generation }

    fun imageChanged() {
        invalidateOperations()
        isSelectionVisible = false
        panoramicMarkers = emptyList()
        // A retained renderer must never accept operations for the replacement image.
        target = null
        clearRasterMarkers()
    }

    fun registerRaster(overlay: GraphicsOverlay, toRasterPoint: (DoubleXY) -> Point?) {
        clearRasterMarkers()
        rasterOverlay = overlay
        target = Target(uiState().activeImageRevision, toRasterPoint) { pixel, symbol, _ ->
            toRasterPoint(pixel)?.let { point ->
                { addRasterMarker(overlay, point, symbol) }
            }
        }
    }

    fun unregisterRaster(owner: (DoubleXY) -> Point?) {
        if (target?.owner !== owner) return
        clearRasterMarkers()
        rasterOverlay = null
        target = null
    }

    fun registerPanorama(revision: Long, width: Int, height: Int): () -> Unit {
        clearRasterMarkers()
        rasterOverlay = null
        val registration = Target(revision, Any()) { pixel, symbol, fromMap ->
            if (width <= 0 || height <= 0 || !pixel.x.isFinite() || !pixel.y.isFinite() ||
                pixel.y !in 0.0..height.toDouble()
            ) null else {
                { panoramicMarkers = panoramicMarkers + PanoramicMarker(pixel, fromMap, symbol) }
            }
        }
        target = registration
        return { if (target === registration) target = null }
    }

    private fun snapshot(): Operation? {
        val state = uiState()
        if (!state.isMarkingLocation) return null
        val image = activeImage() ?: return null
        val currentTarget = target ?: return null
        if (currentTarget.revision != state.activeImageRevision) return null
        return Operation(image, state.activeImageRevision, generation, currentTarget, currentSymbol.clone())
    }

    private fun isCurrent(operation: Operation): Boolean = uiState().let { state ->
        state.isMarkingLocation && state.activeImageRevision == operation.revision &&
                generation == operation.generation && target === operation.target && activeImage() === operation.image
    }

    suspend fun addFromMap(point: Point) {
        currentCoroutineContext().ensureActive()
        // Snapshot once, before either projection or coverage can suspend.
        val operation = snapshot()
        addMapMarker(point, operation?.symbol ?: currentSymbol.clone())
        if (operation == null) return
        val pixel = operation.image.locationToImage(point).getOrThrow()
        currentCoroutineContext().ensureActive()
        if (!isCurrent(operation)) return
        val commit = operation.target.prepare(pixel, operation.symbol, true) ?: return
        if (!canImageSeeLocation(operation.image, point) || !isCurrent(operation)) return
        commit()
    }

    suspend fun addFromRaster(pixel: DoubleXY, point: Point, overlay: GraphicsOverlay) {
        addFromImage(pixel, accepts = { rasterOverlay === overlay }) { symbol ->
            addRasterMarker(overlay, point, symbol)
        }
    }

    suspend fun addFromPanorama(pixel: DoubleXY, revision: Long) {
        if (uiState().activeImageRevision != revision) return
        addFromImage(pixel, accepts = { rasterOverlay == null }) { symbol ->
            panoramicMarkers = panoramicMarkers + PanoramicMarker(pixel, false, symbol)
        }
    }

    private suspend fun addFromImage(
        pixel: DoubleXY,
        accepts: () -> Boolean,
        commit: (SimpleMarkerSymbol) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        val operation = snapshot() ?: return
        if (!accepts()) return
        try {
            val point = operation.image.imageToLocation(pixel).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!isCurrent(operation) || !accepts()) return
            commit(operation.symbol)
            addMapMarker(point, operation.symbol.clone())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            currentCoroutineContext().ensureActive()
            Log.e("OrientedImagery", "imageToLocation failed for (${pixel.x}, ${pixel.y})", error)
        }
    }

    fun addMapMarker(point: Point, symbol: SimpleMarkerSymbol = currentSymbol.clone()) {
        val graphic = Graphic(point, symbol)
        mapOverlay.graphics.add(graphic)
        mapGraphics += graphic
    }

    private fun addRasterMarker(overlay: GraphicsOverlay, point: Point, symbol: SimpleMarkerSymbol) {
        val graphic = Graphic(point, symbol.clone())
        overlay.graphics.add(graphic)
        imageGraphics += graphic
    }

    private suspend fun canImageSeeLocation(image: OrientedImage, point: Point): Boolean {
        val result = layer.searchImages(point, OrientedImageSearchParameters().apply {
            applyHeightFilter = false
            maxResults = -1
        })
        currentCoroutineContext().ensureActive()
        val candidates = result.getOrThrow()
        val idField = layer.featureTable?.objectIdField
        val id = idField?.let { image.attributes[it] }
        if (id != null) return candidates.any { it.attributes[idField] == id }
        val uri = image.dataUri ?: return false
        for (candidate in candidates) {
            if (candidate === image) return true
            val loaded = candidate.load()
            currentCoroutineContext().ensureActive()
            loaded.getOrThrow()
            if (candidate.dataUri == uri) return true
        }
        return false
    }

    private fun clearRasterMarkers() {
        rasterOverlay?.graphics?.removeAll(imageGraphics)
        imageGraphics = emptyList()
    }

    fun clear() {
        invalidateOperations()
        panoramicMarkers = emptyList()
        clearRasterMarkers()
        mapOverlay.graphics.removeAll(mapGraphics)
        mapGraphics = emptyList()
    }

    fun reset() {
        clear()
        target = null
        rasterOverlay = null
        isSelectionVisible = false
    }
}
