/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewer

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ImageDefaults
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageInfo
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerUiState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PointData
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerOptionId
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerSymbols
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
public sealed interface ImageSearchStatus {
    @Immutable
    public data class ImagesFound(
        public val count: Int
    ) : ImageSearchStatus

    @Immutable
    public object NoImagesFound : ImageSearchStatus
}

@Immutable
public sealed interface ImageNavigationStatus {
    @Immutable
    public object Moved : ImageNavigationStatus

    @Immutable
    public object NoActiveImage : ImageNavigationStatus

    @Immutable
    public object BoundaryReached : ImageNavigationStatus
}

/**
 * State for an oriented imagery viewer. Access this state on the main thread.
 *
 * Search and feature fetch share a latest-request-wins policy. Navigation requests are queued
 * and processed one at a time. A search, fetch, or reset invalidates older pending operations,
 * which throw [CancellationException] rather than changing the replacement session.
 */
@Stable
public class OrientedImageryViewerState public constructor(
    private val orientedImageryLayer: OrientedImageryLayer,
    private val graphicsOverlay: GraphicsOverlay,
    private val coroutineScope: CoroutineScope
) {
    /**
     * The title to display for the viewer.
     */
    public val title: String = orientedImageryLayer.name.ifBlank { "Oriented imagery" }

    private val _uiState: MutableState<OrientedImageryViewerUiState> = mutableStateOf(OrientedImageryViewerUiState())
    private var orientedImages: List<OrientedImage> = emptyList()
    private var activeOrientedImage: OrientedImage? = null
    private var activeFootprint: OrientedImageFootprint? = null
    private var activeCameraLocationGraphic: Graphic? = null
    private var inputLocationGraphic: Graphic? = null
    private var sessionGeneration = 0L
    private val navigationMutex = Mutex()
    private var activeIndex = -1
    private var additionalCameraLocationGraphics: List<Graphic> = emptyList()
    private var additionalFootprints: List<OrientedImageFootprint> = emptyList()
    private var footprintUpdateJob: Job? = null
    private var pendingFootprintUpdate: (suspend (OrientedImageFootprint) -> Result<*>)? = null


    private var markerGraphicsInImage: List<Graphic> = emptyList()
    private var markerGraphicsInGeoView: List<Graphic> = emptyList()
    private var markerOverlayInImage: GraphicsOverlay? = null
    private var imageCoordinateToRasterPoint: ((DoubleXY) -> Point?)? = null

    private class ImageMarkerTarget(
        val imageRevision: Long,
        val owner: Any,
        val prepareMarker: (DoubleXY) -> (() -> Unit)?
    )

    internal data class PanoramicMarker(
        val imagePoint: DoubleXY,
        val isFromGeoView: Boolean,
        val symbol: SimpleMarkerSymbol
    )

    private data class MarkerSelectionState(
        val optionId: MarkerOptionId?,
        val symbol: SimpleMarkerSymbol
    )

    private var markerSelection by mutableStateOf(
        MarkerSelectionState(null, ImageDefaults.defaultMarkerSymbol())
    )

    internal val currentMarkerSymbol: SimpleMarkerSymbol
        get() = markerSelection.symbol

    internal val selectedMarkerOptionId: MarkerOptionId?
        get() = markerSelection.optionId

    internal var isMarkerSelectionVisible: Boolean by mutableStateOf(false)
        private set

    private var imageMarkerTarget: ImageMarkerTarget? = null
    private val panoramicMarkers = mutableStateOf<List<PanoramicMarker>>(emptyList())
    private var markerGeneration = 0L

    internal val markerPointsInPanoramicImage: List<PanoramicMarker>
        get() = panoramicMarkers.value

    internal val uiState: OrientedImageryViewerUiState
        get() = _uiState.value

    public val supportsSequentialNavigation: Boolean =
        orientedImageryLayer.supportsSequentialNavigation

    internal val inputLocationData: PointData?
        get() = uiState.inputLocationData

    public val isMarkingLocation: Boolean
        get() = uiState.isMarkingLocation

    public val hasActiveImage: Boolean
        get() = uiState.activeImageInfo != null

    /**
     * Creates a popup for the currently active oriented image.
     *
     * Returns a failed [Result] if there is no active image or popup creation fails.
     */
    public fun createImagePopup(): Result<Popup> {
        val image = activeOrientedImage
            ?: return Result.failure(IllegalStateException("An active oriented image is required to create a popup."))

        return runCatching {
            // TODO: replace with the Popup constructor with OrientedImage when a core bug is fixed
            Popup(Graphic(image.geometry, image.attributes))
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Log.e("OrientedImagery", "Unable to create the image popup", error)
        }
    }

    private fun updateUiState(transform: (OrientedImageryViewerUiState) -> OrientedImageryViewerUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun inputLocation(): Point? = uiState.inputLocationData?.toPoint()

    internal suspend fun imageToLocation(imagePoint: DoubleXY): Result<Point>? =
        activeOrientedImage?.imageToLocation(imagePoint)

    internal suspend fun locationToImage(mapPoint: Point): Result<DoubleXY>? =
        activeOrientedImage?.locationToImage(mapPoint)

    /**
     * Searches for images at the tapped location when [isMarkingLocation] is false.
     * Otherwise, adds a map marker and, if covered by the active image and projected within its
     * bounds, a corresponding marker in the displayed raster or panoramic image.
     *
     * Returns a failed [Result] if searching, projection, or coverage validation fails.
     * Cancellation is rethrown. A map marker may already have been added when an operation fails.
     */
    public suspend fun onSingleTapOnGeoView(mapPoint: Point): Result<Unit> = runCatching {
        currentCoroutineContext().ensureActive()
        if (!isMarkingLocation) {
            searchImages(mapPoint).getOrThrow()
        } else {
            addImageLocationMarker(mapPoint)
            val revision = uiState.activeImageRevision
            val generation = markerGeneration
            val image = activeOrientedImage ?: return@runCatching
            val target = imageMarkerTarget ?: return@runCatching
            fun isCurrent(): Boolean = isMarkingLocation && revision == uiState.activeImageRevision &&
                        target.imageRevision == revision && target === imageMarkerTarget &&
                        generation == markerGeneration

            if (!isCurrent()) return@runCatching
            val imagePoint = image.locationToImage(mapPoint).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return@runCatching
            val addMarker = target.prepareMarker(imagePoint) ?: return@runCatching
            if (!canImageSeeLocation(image, mapPoint) || !isCurrent()) return@runCatching
            addMarker()
        }
        currentCoroutineContext().ensureActive()
    }.onFailure { error ->
        if (error is CancellationException) throw error
        currentCoroutineContext().ensureActive()
    }

    private suspend fun canImageSeeLocation(image: OrientedImage, point: Point): Boolean {
        val result = orientedImageryLayer.searchImages(point, OrientedImageSearchParameters().apply {
                applyHeightFilter = false
                // The active image may rank below the default result limit.
                maxResults = -1
            })
        currentCoroutineContext().ensureActive()
        val candidates = result.getOrThrow()
        val objectIdField = orientedImageryLayer.featureTable?.objectIdField
        val objectId = objectIdField?.let { image.attributes[it] }
        if (objectId != null) {
            return candidates.any { it.attributes[objectIdField] == objectId }
        }
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

    internal fun registerMarkerOverlayInImage(
        overlay: GraphicsOverlay,
        toRasterPoint: (DoubleXY) -> Point?
    ) {
        markerOverlayInImage?.graphics?.removeAll(markerGraphicsInImage)
        markerGraphicsInImage = emptyList()
        markerOverlayInImage = overlay
        imageCoordinateToRasterPoint = toRasterPoint
        imageMarkerTarget = ImageMarkerTarget(uiState.activeImageRevision, toRasterPoint) { imagePoint ->
                val rasterPoint = toRasterPoint(imagePoint)
                if (rasterPoint == null) null else {
                    {
                        val graphic = Graphic(
                            geometry = rasterPoint,
                            symbol = currentMarkerSymbol
                        )
                        overlay.graphics.add(graphic)
                        markerGraphicsInImage += graphic
                    }
                }
            }
    }

    internal fun unregisterMarkerOverlayInImage(toRasterPoint: (DoubleXY) -> Point?) {
        if (imageCoordinateToRasterPoint !== toRasterPoint) return
        markerOverlayInImage?.graphics?.removeAll(markerGraphicsInImage)
        markerGraphicsInImage = emptyList()
        markerOverlayInImage = null
        imageCoordinateToRasterPoint = null
        if (imageMarkerTarget?.owner === toRasterPoint) imageMarkerTarget = null
    }

    /** Registers a ready panorama and returns an ownership-aware disposal callback. */
    internal fun registerMarkerInPanoramicImage(
        imageRevision: Long,
        width: Int,
        height: Int
    ): () -> Unit {
        val target = ImageMarkerTarget(imageRevision, Any()) { imagePoint ->
            val symbol = currentMarkerSymbol.clone()
            // Longitude wraps at the panorama seam; latitude must stay within the image.
            if (width <= 0 || height <= 0 || !imagePoint.x.isFinite() ||
                !imagePoint.y.isFinite() || imagePoint.y !in 0.0..height.toDouble()
            ) null else {
                {
                    panoramicMarkers.value = panoramicMarkers.value + PanoramicMarker(imagePoint, true, symbol)
                }
            }
        }
        imageMarkerTarget = target
        return {
            if (imageMarkerTarget === target) imageMarkerTarget = null
        }
    }

    internal fun setInputLocation(point: Point?) {
        setInputLocationData(point?.let { pointData ->
            PointData(
                x = pointData.x,
                y = pointData.y,
                z = pointData.z,
                spatialReferenceWkid = pointData.spatialReference?.wkid
            )
        })
    }

    /**
     * Returns a [Result] indicating whether oriented images were found at the specified map location.
     * If images are found, the first image is loaded and set as the active image.
     * If no images are found, the active image is cleared.
     *
     * Returns a failed [Result] if searching or loading fails.
     * Cancellation is rethrown. The active image may already have been changed when an operation fails.
     */
    public suspend fun searchImages(lastMapLocation: Point): Result<ImageSearchStatus> {
        currentCoroutineContext().ensureActive()
        resetAll()
        val session = sessionGeneration
        setInputLocation(lastMapLocation)
        return inSession(session) {
            val results = orientedImageryLayer.searchImages(
                lastMapLocation,
                OrientedImageSearchParameters().apply { applyHeightFilter = false }
            ).getInSession(session)
            if (results.isEmpty()) {
                ImageSearchStatus.NoImagesFound
            } else {
                val image = results.first()
                loadImage(image, session)
                orientedImages = results
                activeIndex = 0
                setActiveOrientedImage(image)
                ImageSearchStatus.ImagesFound(results.size)
            }
        }
    }

    /**
     * Fetches the oriented image for the specified feature and sets it as the active image.
     *
     * Returns a failed [Result] if fetching or loading fails.
     * Cancellation is rethrown. The active image may already have been changed when an operation fails.
     */
    public suspend fun fetchImage(feature: Feature): Result<Unit> {
        currentCoroutineContext().ensureActive()
        resetAll()
        val session = sessionGeneration
        return inSession(session) {
            val image = orientedImageryLayer.fetchImageForFeature(feature).getInSession(session)
            loadImage(image, session)
            setInputLocation(feature.geometry as? Point)
            orientedImages = listOf(image)
            activeIndex = 0
            setActiveOrientedImage(image)
        }
    }

    /**
     * Navigates to the next oriented image.
     *
     * Returns a [Result] indicating the navigation status.
     */
    public suspend fun navigateNextImage(): Result<ImageNavigationStatus> = navigate(SequenceStep.Next, 1)

    /**
     * Navigates to the previous oriented image.
     *
     * Returns a [Result] indicating the navigation status.
     */
    public suspend fun navigatePreviousImage(): Result<ImageNavigationStatus> = navigate(SequenceStep.Previous, -1)

    private suspend fun ensureSession(session: Long) {
        currentCoroutineContext().ensureActive()
        if (session != sessionGeneration) throw CancellationException("The image session was superseded.")
    }

    private suspend fun <T> Result<T>.getInSession(session: Long): T {
        ensureSession(session)
        return getOrThrow()
    }

    private suspend fun <T> inSession(session: Long, operation: suspend () -> T): Result<T> = try {
        ensureSession(session)
        Result.success(operation())
    } catch (error: Exception) {
        ensureSession(session)
        if (error is CancellationException) throw error
        Log.e("OrientedImagery", "Image operation failed", error)
        Result.failure(error)
    }

    private suspend fun loadImage(image: OrientedImage, session: Long) {
        val result = image.load()
        ensureSession(session)
        result.onFailure { error ->
            if (error is CancellationException) throw error
            logOrientedImageLoadFailure(error, image)
        }.getOrThrow()
    }

    private suspend fun navigate(step: SequenceStep, offset: Int): Result<ImageNavigationStatus> {
        // Capture before waiting: queued commands belong to the session that received them.
        val session = sessionGeneration
        return navigationMutex.withLock {
            inSession(session) {
                if (!hasActiveImage) ImageNavigationStatus.NoActiveImage
                else if (uiState.isSequentialNavigationEnabled) fetchAdjacentImage(step, session)
                else showImageInList(offset, session)
            }
        }
    }

    /**
     * Toggles the visibility of the active footprint.
     */
    public fun toggleActiveFootprint() {
        val shouldShow = !uiState.isActiveFootprintVisible
        updateUiState { uiState -> uiState.copy(isActiveFootprintVisible = shouldShow) }

        if (shouldShow) {
            showActiveFootprint()
        } else {
            activeFootprint?.let { footprint ->
                orientedImageryLayer.visibleFootprints.remove(footprint)
            }
            activeCameraLocationGraphic?.isVisible = false
        }
    }

    /**
     * Toggles the visibility of the additional footprints.
     */
    public fun toggleAdditionalFootprints() {
        val shouldShow = !uiState.isAdditionalFootprintsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalFootprintsVisible = shouldShow) }

        if (shouldShow) {
            showAdditionalFootprints()
        } else {
            orientedImageryLayer.visibleFootprints.removeAll(additionalFootprints)
        }
    }

    /**
     * Toggles the visibility of the additional camera locations.
     */
    public fun toggleAdditionalCameraLocations() {
        val shouldShow = !uiState.isAdditionalCameraLocationsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalCameraLocationsVisible = shouldShow) }

        if (shouldShow) {
            showAdditionalCameraLocations()
        } else {
            graphicsOverlay.graphics.removeAll(additionalCameraLocationGraphics)
        }
    }

    /**
     * Toggles the sequential navigation mode.
     */
    public fun toggleSequentialNavigation() {
        setSequentialNavigationEnabled(!uiState.isSequentialNavigationEnabled)
    }

    internal fun updateFootprints(imageCoordinates: List<DoubleXY>) {
        updateActiveFootprint { footprint -> footprint.updateFootprint(imageCoordinates) }
    }

    internal fun updateFootprints(yaw: Double, pitch: Double, horizontalFov: Double, verticalFov: Double) {
        updateActiveFootprint { footprint -> footprint.updateFootprint(yaw, pitch, horizontalFov, verticalFov) }
    }

    private fun updateActiveFootprint(update: suspend (OrientedImageFootprint) -> Result<*>) {
        if (!uiState.isActiveFootprintVisible) return

        activeFootprint?.let { footprint ->
            pendingFootprintUpdate = update
            if (footprintUpdateJob?.isActive == true) {
                return
            }

            footprintUpdateJob = coroutineScope.launch {
                while (true) {
                    val updateToApply = pendingFootprintUpdate ?: break
                    pendingFootprintUpdate = null

                    val result = updateToApply(footprint)
                    currentCoroutineContext().ensureActive()
                    if (activeFootprint !== footprint) break
                    result.onSuccess {
                        Log.i(
                            "OrientedImageryViewerState",
                            "Footprint updated successfully for image: ${footprint.orientedImage.dataUri}"
                        )
                    }.onFailure {
                        if (it is CancellationException) throw it
                        Log.i(
                            "OrientedImageryViewerState",
                            "Failed to update footprint for image: ${footprint.orientedImage.dataUri}",
                            it
                        )
                    }
                }
            }
        }
    }

    /**
     * Returns `true` if the active footprint is visible.
     */
    public fun isActiveFootprintVisible(): Boolean = uiState.isActiveFootprintVisible

    /**
     * Adds a marker at the current location.
     */
    public fun addMarker() {
        ++markerGeneration
        updateUiState { uiState ->
            uiState.copy(isMarkingLocation = !uiState.isMarkingLocation)
        }
    }

    internal fun addImageLocationMarker(point: Point) {
        val graphic = Graphic(point, currentMarkerSymbol)
        graphicsOverlay.graphics.add(graphic)
        markerGraphicsInGeoView += graphic
    }

    internal suspend fun addMarkerToRasterImage(
        imagePoint: DoubleXY,
        rasterPoint: Point,
        rasterGraphicsOverlay: GraphicsOverlay
    ) {
        if (!isMarkingLocation) return
        currentCoroutineContext().ensureActive()
        val imageRevision = uiState.activeImageRevision
        val generation = markerGeneration
        val target = imageMarkerTarget
        val symbol = currentMarkerSymbol.clone()
        val result = imageToLocation(imagePoint)
        currentCoroutineContext().ensureActive()
        if (result != null) {
            result.onSuccess { point ->
                if (!isMarkingLocation ||
                    uiState.activeImageRevision != imageRevision || generation != markerGeneration ||
                    target !== imageMarkerTarget || markerOverlayInImage !== rasterGraphicsOverlay
                ) return@onSuccess

                val graphic = Graphic(
                    geometry = rasterPoint,
                    symbol = symbol
                )
                rasterGraphicsOverlay.graphics.add(graphic)
                markerGraphicsInImage += graphic
                addImageLocationMarker(point)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.e(
                    "RasterImage",
                    "imageToLocation failed for image coordinates: (${imagePoint.x}, ${imagePoint.y}) with error: ${error.message}"
                )
            }
        } else {
            Log.e(
                "RasterImage",
                "imageToLocation returned no active oriented image for image coordinates: (${imagePoint.x}, ${imagePoint.y})"
            )
        }
    }

    internal suspend fun addMarkerToPanoramicImage(imagePoint: DoubleXY, imageRevision: Long) {
        if (!isMarkingLocation || uiState.activeImageRevision != imageRevision) return
        val image = activeOrientedImage ?: return
        val generation = markerGeneration
        val symbol = currentMarkerSymbol.clone()
        currentCoroutineContext().ensureActive()
        val result = image.imageToLocation(imagePoint)
        currentCoroutineContext().ensureActive()
        result.onSuccess { point ->
            if (!isMarkingLocation || uiState.activeImageRevision != imageRevision ||
                generation != markerGeneration
            ) return@onSuccess

            addImageLocationMarker(point)
            panoramicMarkers.value = panoramicMarkers.value + PanoramicMarker(imagePoint, false, symbol)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Log.e(
                "PanoramicImage",
                "imageToLocation failed for image coordinates: (${imagePoint.x}, ${imagePoint.y})",
                error
            )
        }
    }

    public fun selectMarker() {
        isMarkerSelectionVisible = true
    }

    internal fun selectMarkerOption(optionId: MarkerOptionId) {
        val option = MarkerSymbols.options.first { it.id == optionId }
        markerSelection = MarkerSelectionState(option.id, option.symbol.clone())
        dismissMarkerSelection()
    }

    internal fun selectMarkerSymbol(symbol: SimpleMarkerSymbol) {
        // Only registered prototypes carry palette identity; never infer it from appearance.
        val optionId = MarkerSymbols.options.firstOrNull { it.symbol === symbol }?.id
        markerSelection = MarkerSelectionState(optionId, symbol.clone())
        dismissMarkerSelection()
    }

    internal fun dismissMarkerSelection() {
        isMarkerSelectionVisible = false
    }

    public fun clearAllMarkers() {
        ++markerGeneration
        panoramicMarkers.value = emptyList()
        markerOverlayInImage?.graphics?.removeAll(markerGraphicsInImage)
        graphicsOverlay.graphics.removeAll(markerGraphicsInGeoView)
        markerGraphicsInImage = emptyList()
        markerGraphicsInGeoView = emptyList()
    }

    /**
     * Resets the viewer state to its initial state.
     *
     * Invalidates pending search, fetch, and navigation operations. Removes only graphics and
     * footprints created by this viewer, retaining application-owned objects in shared collections.
     */
    public fun resetAll() {
        ++sessionGeneration
        cancelFootprintUpdateJob()
        activeFootprint?.let { orientedImageryLayer.visibleFootprints.remove(it) }
        orientedImageryLayer.visibleFootprints.removeAll(additionalFootprints)
        activeCameraLocationGraphic?.let { graphicsOverlay.graphics.remove(it) }
        inputLocationGraphic?.let { graphicsOverlay.graphics.remove(it) }
        inputLocationGraphic = null
        graphicsOverlay.graphics.removeAll(additionalCameraLocationGraphics)
        setActiveOrientedImage(null)
        activeFootprint = null
        activeIndex = -1
        activeCameraLocationGraphic = null
        setInputLocation(null)
        updateUiState { uiState ->
            uiState.copy(
                isMarkingLocation = false,
                isActiveFootprintVisible = false,
                isAdditionalFootprintsVisible = false,
                isAdditionalCameraLocationsVisible = false,
                activeImageInfo = null,
                inputLocationData = null
            )
        }
        clearAllMarkers()
        markerOverlayInImage = null
        imageCoordinateToRasterPoint = null
        imageMarkerTarget = null
        orientedImages = emptyList()
        additionalFootprints = emptyList()
        additionalCameraLocationGraphics = emptyList()
    }

    /**
     * Sets whether sequential navigation is enabled for the viewer.
     */
    public fun setSequentialNavigationEnabled(enabled: Boolean) {
        updateUiState { uiState -> uiState.copy(isSequentialNavigationEnabled = enabled) }
    }

    private fun setActiveOrientedImage(orientedImage: OrientedImage?) {
        activeOrientedImage = orientedImage
        setActiveImageInfo(orientedImage?.let { image ->
            OrientedImageInfo(
                dataUri = image.dataUri,
                type = image.type,
                cameraLocationData = (image.geometry as? Point)?.let { camera ->
                    PointData(camera.x, camera.y, camera.z, camera.spatialReference?.wkid)
                }
            )
        })
    }

    private fun setActiveImageInfo(value: OrientedImageInfo?) {
        dismissMarkerSelection()
        panoramicMarkers.value = emptyList()
        updateUiState { uiState ->
            uiState.copy(
                activeImageInfo = value,
                activeImageRevision = uiState.activeImageRevision + 1
            )
        }
    }

    private fun setInputLocationData(value: PointData?) {
        updateUiState { uiState -> uiState.copy(inputLocationData = value) }
    }

    private suspend fun fetchAdjacentImage(step: SequenceStep, session: Long): ImageNavigationStatus {
        val currentImage = activeOrientedImage ?: return ImageNavigationStatus.NoActiveImage
        val image = orientedImageryLayer.fetchAdjacentImage(currentImage, step).getInSession(session)
        loadImage(image, session)
        setActiveOrientedImage(image)
        resetActive()
        return ImageNavigationStatus.Moved
    }

    private suspend fun showImageInList(offset: Int, session: Long): ImageNavigationStatus {
        val targetIndex = activeIndex + offset
        val image = orientedImages.getOrNull(targetIndex) ?: return ImageNavigationStatus.BoundaryReached
        loadImage(image, session)
        activeIndex = targetIndex
        setActiveOrientedImage(image)
        resetActive()
        return ImageNavigationStatus.Moved
    }

    private fun showActiveFootprint() {
        if (activeFootprint == null) {
            activeFootprint = createActiveFootprint()
        } else {
            orientedImageryLayer.visibleFootprints.add(activeFootprint!!)
            activeCameraLocationGraphic?.isVisible = true
        }
    }

    private fun showAdditionalFootprints() {
        if (additionalFootprints.isEmpty()) {
            additionalFootprints = orientedImages
                .filterIndexed { index, _ -> index != activeIndex }
                .map { orientedImage ->
                    OrientedImageFootprint(orientedImage).apply {
                        fillColor = ImageDefaults.ADDITIONAL_FOOTPRINT_COLOR
                        outlineColor = Color.transparent
                    }
                }
        }
        orientedImageryLayer.visibleFootprints.addAll(additionalFootprints)
    }

    private fun showAdditionalCameraLocations() {
        if (additionalCameraLocationGraphics.isEmpty()) {
            additionalCameraLocationGraphics = orientedImages
                .filterIndexed { index, _ -> index != activeIndex }
                .map { orientedImage ->
                    Graphic(
                        geometry = orientedImage.geometry,
                        symbol = ImageDefaults.additionalCameraLocationSymbol()
                    )
                }
        }
        graphicsOverlay.graphics.addAll(additionalCameraLocationGraphics)
    }

    private fun resetActive() {
        cancelFootprintUpdateJob()
        activeFootprint?.let { footprint ->
            orientedImageryLayer.visibleFootprints.remove(footprint)
        }
        activeFootprint = null
        activeCameraLocationGraphic?.let { graphic ->
            graphicsOverlay.graphics.remove(graphic)
        }
        activeCameraLocationGraphic = null
        inputLocationGraphic?.let { graphicsOverlay.graphics.remove(it) }
        inputLocationGraphic = null
        if (additionalFootprints.isNotEmpty()) {
            orientedImageryLayer.visibleFootprints.removeAll(additionalFootprints)
            additionalFootprints = emptyList()
        }
        if (additionalCameraLocationGraphics.isNotEmpty()) {
            graphicsOverlay.graphics.removeAll(additionalCameraLocationGraphics)
            additionalCameraLocationGraphics = emptyList()
        }

        if (uiState.isActiveFootprintVisible) showActiveFootprint()
        if (uiState.isAdditionalFootprintsVisible) showAdditionalFootprints()
        if (uiState.isAdditionalCameraLocationsVisible) showAdditionalCameraLocations()
    }

    private fun cancelFootprintUpdateJob() {
        footprintUpdateJob?.cancel()
        footprintUpdateJob = null
        pendingFootprintUpdate = null
    }

    private fun logOrientedImageLoadFailure(
        error: Throwable,
        orientedImage: OrientedImage,
        messagePrefix: String = "Failed to load oriented image"
    ) {
        val resolvedUri = orientedImage.dataUri
        val originalUri = orientedImage.attributes["ImagePath"]?.toString().orEmpty()
        val originalImageHost = runCatching { originalUri.toUri().host }.getOrNull().orEmpty()

        if (originalUri.startsWith("http://", ignoreCase = true)) {
            val hostSuffix = originalImageHost.takeIf { it.isNotBlank() }?.let { " for host '$it'" }.orEmpty()
            Log.w(
                "OrientedImagery",
                "Detected cleartext HTTP oriented image source$hostSuffix. Android may block this unless the app allows cleartext traffic with usesCleartextTraffic or a network security config. Original URI: $originalUri"
            )
        }

        if (resolvedUri.isNullOrBlank()) {
            Log.w(
                "OrientedImagery",
                "Oriented image load failed before the SDK resolved a data URI. Original URI: $originalUri"
            )
        }

        Log.i(
            "OrientedImagery",
            "$messagePrefix: ${error.message}; URI: ${resolvedUri.orEmpty()}; Original URI: $originalUri"
        )
    }

    private fun createActiveFootprint(): OrientedImageFootprint {
        val activeImage = requireNotNull(activeOrientedImage) {
            "An active oriented image is required to create a footprint."
        }

        // Never adopt a footprint owned by the application, even if its image URI matches.
        return OrientedImageFootprint(activeImage).also { footprint ->
            Log.i("OrientedImageryViewerState", "Creating new footprint for active image: ${activeImage.dataUri}")
            footprint.outlineColor = Color.transparent
            orientedImageryLayer.visibleFootprints.add(footprint)

            inputLocationGraphic?.let { graphicsOverlay.graphics.remove(it) }
            inputLocationGraphic = null
            if (!uiState.isSequentialNavigationEnabled) {
                inputLocation()?.let { inputLoc ->
                    val graphicInputLocation = Graphic(inputLoc, ImageDefaults.inputLocationSymbol())
                    inputLocationGraphic = graphicInputLocation
                    graphicsOverlay.graphics.add(graphicInputLocation)
                }
            }
            activeCameraLocationGraphic = Graphic(activeImage.geometry, ImageDefaults.activeCameraLocationSymbol())
            graphicsOverlay.graphics.add(activeCameraLocationGraphic!!)
        }
    }
}
