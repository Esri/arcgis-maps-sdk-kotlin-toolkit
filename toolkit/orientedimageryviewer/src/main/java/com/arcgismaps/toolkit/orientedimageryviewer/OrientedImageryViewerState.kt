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

package com.arcgismaps.toolkit.orientedimageryviewer

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.internal.FootprintManager
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ImageSession
import com.arcgismaps.toolkit.orientedimageryviewer.internal.MarkerCoordinator
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageInfo
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerUiState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PanoramicMarker
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PointData
import com.arcgismaps.toolkit.orientedimageryviewer.internal.toPointData
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerOptionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Status of an oriented imagery search operation.
 */
@Immutable
public sealed interface ImageSearchStatus {
    @Immutable
    public data class ImagesFound(
        public val count: Int
    ) : ImageSearchStatus

    @Immutable
    public object NoImagesFound : ImageSearchStatus
}

/**
 * Status of an oriented imagery navigation operation.
 */
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
 * Image sessions, marker operations, and footprint resources have independent internal owners.
 */
@Stable
public class OrientedImageryViewerState public constructor(
    orientedImageryLayer: OrientedImageryLayer,
    graphicsOverlay: GraphicsOverlay,
    coroutineScope: CoroutineScope
) {
    /**
     * The title to display for the viewer.
     */
    public val title: String = orientedImageryLayer.name.ifBlank { "Oriented imagery" }

    private val _uiState = mutableStateOf(OrientedImageryViewerUiState())
    private val session: ImageSession = ImageSession(
        orientedImageryLayer,
        onImageChanged = ::publishActiveImage,
        onInputLocationChanged = ::setInputLocation,
        onNavigated = { footprints.imageChanged() }
    )
    private val markers: MarkerCoordinator = MarkerCoordinator(
        orientedImageryLayer, graphicsOverlay,
        activeImage = { session.activeImage },
        uiState = { uiState }
    )
    private val footprints: FootprintManager = FootprintManager(
        orientedImageryLayer, graphicsOverlay, coroutineScope,
        activeImage = { session.activeImage },
        additionalImages = { session.additionalImages },
        inputLocation = { uiState.inputLocationData?.toPoint() },
        uiState = { uiState }
    )

    internal val currentMarkerSymbol: SimpleMarkerSymbol
        get() = markers.currentSymbol

    internal val selectedMarkerOptionId: MarkerOptionId?
        get() = markers.selectedOptionId

    internal val isMarkerSelectionVisible: Boolean
        get() = markers.isSelectionVisible

    internal val markerPointsInPanoramicImage: List<PanoramicMarker>
        get() = markers.panoramicMarkers

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
        val image = session.activeImage
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

    internal suspend fun imageToLocation(imagePoint: DoubleXY): Result<Point>? =
        session.activeImage?.imageToLocation(imagePoint)

    internal suspend fun locationToImage(mapPoint: Point): Result<DoubleXY>? =
        session.activeImage?.locationToImage(mapPoint)

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
            markers.addFromMap(mapPoint)
        }
        currentCoroutineContext().ensureActive()
    }.onFailure { error ->
        if (error is CancellationException) throw error
        currentCoroutineContext().ensureActive()
    }

    internal fun registerMarkerOverlayInImage(
        overlay: GraphicsOverlay,
        toRasterPoint: (DoubleXY) -> Point?
    ) {
        markers.registerRaster(overlay, toRasterPoint)
    }

    internal fun unregisterMarkerOverlayInImage(toRasterPoint: (DoubleXY) -> Point?) {
        markers.unregisterRaster(toRasterPoint)
    }

    /** Registers a ready panorama and returns an ownership-aware disposal callback. */
    internal fun registerMarkerInPanoramicImage(
        imageRevision: Long,
        width: Int,
        height: Int
    ): () -> Unit = markers.registerPanorama(imageRevision, width, height)

    internal fun setInputLocation(point: Point?) {
        updateUiState { it.copy(inputLocationData = point?.toPointData()) }
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
        setInputLocation(lastMapLocation)
        return session.search(lastMapLocation)
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
        return session.fetch(feature)
    }

    /**
     * Navigates to the next oriented image.
     *
     * Returns a [Result] indicating the navigation status.
     */
    public suspend fun navigateNextImage(): Result<ImageNavigationStatus> =
        session.navigate(SequenceStep.Next, 1) { uiState.isSequentialNavigationEnabled }

    /**
     * Navigates to the previous oriented image.
     *
     * Returns a [Result] indicating the navigation status.
     */
    public suspend fun navigatePreviousImage(): Result<ImageNavigationStatus> =
        session.navigate(SequenceStep.Previous, -1) { uiState.isSequentialNavigationEnabled }

    /**
     * Toggles the visibility of the active footprint.
     */
    public fun toggleActiveFootprint() {
        val shouldShow = !uiState.isActiveFootprintVisible
        updateUiState { uiState -> uiState.copy(isActiveFootprintVisible = shouldShow) }

        footprints.setActiveVisible(shouldShow)
    }

    /**
     * Toggles the visibility of the additional footprints.
     */
    public fun toggleAdditionalFootprints() {
        val shouldShow = !uiState.isAdditionalFootprintsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalFootprintsVisible = shouldShow) }

        footprints.setAdditionalFootprintsVisible(shouldShow)
    }

    /**
     * Toggles the visibility of the additional camera locations.
     */
    public fun toggleAdditionalCameraLocations() {
        val shouldShow = !uiState.isAdditionalCameraLocationsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalCameraLocationsVisible = shouldShow) }

        footprints.setAdditionalCamerasVisible(shouldShow)
    }

    /**
     * Toggles the sequential navigation mode.
     */
    public fun toggleSequentialNavigation() {
        setSequentialNavigationEnabled(!uiState.isSequentialNavigationEnabled)
    }

    internal fun updateFootprints(imageCoordinates: List<DoubleXY>) {
        footprints.update { footprint -> footprint.updateFootprint(imageCoordinates) }
    }

    internal fun updateFootprints(yaw: Double, pitch: Double, horizontalFov: Double, verticalFov: Double) {
        footprints.update { footprint -> footprint.updateFootprint(yaw, pitch, horizontalFov, verticalFov) }
    }

    /**
     * Returns `true` if the active footprint is visible.
     */
    public fun isActiveFootprintVisible(): Boolean = uiState.isActiveFootprintVisible

    /**
     * Adds a marker at the current location.
     */
    public fun addMarker() {
        markers.invalidateOperations()
        updateUiState { uiState ->
            uiState.copy(isMarkingLocation = !uiState.isMarkingLocation)
        }
    }

    internal fun addImageLocationMarker(point: Point) {
        markers.addMapMarker(point)
    }

    internal suspend fun addMarkerToRasterImage(
        imagePoint: DoubleXY,
        rasterPoint: Point,
        rasterGraphicsOverlay: GraphicsOverlay
    ) {
        markers.addFromRaster(imagePoint, rasterPoint, rasterGraphicsOverlay)
    }

    internal suspend fun addMarkerToPanoramicImage(imagePoint: DoubleXY, imageRevision: Long) {
        markers.addFromPanorama(imagePoint, imageRevision)
    }

    public fun selectMarker() {
        markers.isSelectionVisible = true
    }

    internal fun selectMarkerOption(optionId: MarkerOptionId) {
        markers.selectOption(optionId)
    }

    internal fun selectMarkerSymbol(symbol: SimpleMarkerSymbol) {
        markers.selectSymbol(symbol)
    }

    internal fun dismissMarkerSelection() {
        markers.isSelectionVisible = false
    }

    public fun clearAllMarkers() {
        markers.clear()
    }

    /**
     * Resets the viewer state to its initial state.
     *
     * Invalidates pending search, fetch, and navigation operations. Removes only graphics and
     * footprints created by this viewer, retaining application-owned objects in shared collections.
     */
    public fun resetAll() {
        footprints.reset()
        session.reset()
        markers.reset()
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
    }

    /**
     * Sets whether sequential navigation is enabled for the viewer.
     */
    public fun setSequentialNavigationEnabled(enabled: Boolean) {
        updateUiState { uiState -> uiState.copy(isSequentialNavigationEnabled = enabled) }
        footprints.navigationModeChanged()
    }

    private fun publishActiveImage(orientedImage: OrientedImage?) {
        markers.imageChanged()
        val info = orientedImage?.let { image ->
            OrientedImageInfo(
                dataUri = image.dataUri,
                type = image.type,
                cameraLocationData = (image.geometry as? Point)?.toPointData()
            )
        }
        updateUiState { uiState ->
            uiState.copy(
                activeImageInfo = info,
                activeImageRevision = uiState.activeImageRevision + 1
            )
        }
    }
}
