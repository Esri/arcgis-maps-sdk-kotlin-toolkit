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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.GeoViewProxy
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.internal.FootprintManager
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ImageSession
import com.arcgismaps.toolkit.orientedimageryviewer.internal.MarkerCoordinator
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageInfo
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerUiState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PanoramicMarker
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PointData
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ViewerAppearance
import com.arcgismaps.toolkit.orientedimageryviewer.internal.toPointData
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerOptionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Mutually exclusive operations performed when the user taps the GeoView. */
public enum class GeoViewInteractionStatus {
    /** Ignores GeoView taps. */
    None,

    /** Searches for images covering the tapped location. */
    FindImages,

    /** Identifies and displays the first image feature at the tapped screen coordinate. */
    DisplayImage,

    /** Adds markers at the tapped location. */
    AddMarkers
}

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
 * A newly constructed or reset state displays guidance for selecting an image. Image request
 * progress is displayed until identification, retrieval, and loading finish. Failures and empty
 * results display a persistent message instead of image content.
 *
 * Search and feature fetch share a latest-request-wins policy. Navigation requests are queued
 * and processed one at a time. A search, fetch, or reset invalidates older pending operations,
 * which throw [CancellationException] rather than changing the replacement session.
 * Image sessions, marker operations, and footprint resources have independent internal owners.
 *
 * @param geoViewProxy The proxy for the GeoView whose taps are handled by this viewer.
 * @param orientedImageryLayer The oriented imagery layer associated with this viewer state.
 * @param graphicsOverlay The graphics overlay used to display camera locations and footprints.
 * @param coroutineScope The coroutine scope for managing asynchronous operations.
 */
@Stable
public class OrientedImageryViewerState internal constructor(
    private val geoViewProxy: GeoViewProxy,
    private val orientedImageryLayer: OrientedImageryLayer,
    graphicsOverlay: GraphicsOverlay,
    coroutineScope: CoroutineScope,
    createFootprint: (OrientedImage) -> OrientedImageFootprint
) {
    public constructor(
        geoViewProxy: GeoViewProxy,
        orientedImageryLayer: OrientedImageryLayer,
        graphicsOverlay: GraphicsOverlay,
        coroutineScope: CoroutineScope
    ) : this(geoViewProxy, orientedImageryLayer, graphicsOverlay, coroutineScope, ::OrientedImageFootprint)

    /**
     * The title to display for the viewer.
     */
    public val title: String = orientedImageryLayer.name.ifBlank { "Oriented imagery" }

    private val _uiState = mutableStateOf(OrientedImageryViewerUiState())
    private val _isViewerVisible = mutableStateOf(true)
    internal var appearance: ViewerAppearance by mutableStateOf(ViewerAppearance())
        private set

    internal fun updateAppearance(value: ViewerAppearance) {
        if (appearance == value) return
        appearance = value
        footprints.updateAppearance(value)
    }

    /** Whether the host should display the viewer. Image-mode taps reopen a dismissed viewer. */
    public val isViewerVisible: Boolean
        get() = _isViewerVisible.value

    // Failed image requests clear the displayed checks, not these user preferences.
    private var preferredActiveFootprintVisible = uiState.isActiveFootprintVisible
    private var preferredAdditionalFootprintsVisible = uiState.isAdditionalFootprintsVisible
    private var preferredAdditionalCameraLocationsVisible = uiState.isAdditionalCameraLocationsVisible
    private var interactionRevision: Long = 0
    private var imageRequestRevision: Long = 0
    private val session: ImageSession = ImageSession(
        orientedImageryLayer,
        onImageChanged = ::publishActiveImage,
        onInputLocationChanged = ::setInputLocation,
        onImageActivated = { footprints.imageChanged() },
        scope = coroutineScope,
        isSequential = { uiState.isSequentialNavigationEnabled },
        onAdjacentImageChanged = { step, available, error ->
            updateUiState {
                if (step == SequenceStep.Previous) {
                    it.copy(canNavigatePrevious = available, previousImageError = error)
                } else {
                    it.copy(canNavigateNext = available, nextImageError = error)
                }
            }
        }
    )
    private val markers: MarkerCoordinator = MarkerCoordinator(
        graphicsOverlay,
        activeImage = { session.activeImage },
        uiState = { uiState }
    )
    private val footprints: FootprintManager = FootprintManager(
        orientedImageryLayer, graphicsOverlay, coroutineScope,
        activeImage = { session.activeImage },
        additionalImages = { session.additionalImages },
        inputLocation = { uiState.inputLocationData?.toPoint() },
        uiState = { uiState },
        createFootprint = createFootprint
    ).also { it.updateAppearance(appearance) }

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

    internal val searchResultImages: List<OrientedImage>
        get() {
            // Observe replacement sessions without exposing SDK objects in the immutable UI snapshot.
            uiState.imageSessionRevision
            return session.images
        }

    public val supportsSequentialNavigation: Boolean =
        orientedImageryLayer.supportsSequentialNavigation

    internal val inputLocationData: PointData?
        get() = uiState.inputLocationData

    public val isMarkingLocation: Boolean
        get() = uiState.isMarkingLocation

    /**
     * The operation performed on a GeoView tap. Defaults to [GeoViewInteractionStatus.FindImages].
     * Changing this status invalidates pending identify and marker operations.
     * Assigning an image mode selects it and unselects the other image option.
     * Assigning [GeoViewInteractionStatus.None] clears both image selections.
     * Assigning [GeoViewInteractionStatus.AddMarkers] clears both image selections and enables marker placement.
     */
    public var geoViewInteractionStatus: GeoViewInteractionStatus
        get() = uiState.geoViewInteractionStatus
        set(value) {
            when (value) {
                GeoViewInteractionStatus.FindImages, GeoViewInteractionStatus.DisplayImage ->
                    setImageModeSelected(value, true)

                GeoViewInteractionStatus.AddMarkers -> updateInteractionState {
                    it.copy(
                        isFindImagesSelected = false,
                        isDisplayImageSelected = false,
                        imageInteractionStatus = GeoViewInteractionStatus.None,
                        isMarkingLocation = true
                    )
                }

                GeoViewInteractionStatus.None -> updateInteractionState {
                    it.copy(
                        isFindImagesSelected = false,
                        isDisplayImageSelected = false,
                        imageInteractionStatus = GeoViewInteractionStatus.None,
                        isMarkingLocation = false
                    )
                }
            }
        }

    /** Selects at most one image mode; unselecting the active option leaves neither selected. */
    internal fun setImageModeSelected(mode: GeoViewInteractionStatus, selected: Boolean) {
        require(mode == GeoViewInteractionStatus.FindImages || mode == GeoViewInteractionStatus.DisplayImage)
        updateInteractionState { current ->
            val findImages = if (mode == GeoViewInteractionStatus.FindImages) {
                selected
            } else {
                !selected && current.isFindImagesSelected
            }
            val displayImage = if (mode == GeoViewInteractionStatus.DisplayImage) {
                selected
            } else {
                !selected && current.isDisplayImageSelected
            }
            val activeMode = when {
                findImages -> GeoViewInteractionStatus.FindImages
                displayImage -> GeoViewInteractionStatus.DisplayImage
                else -> GeoViewInteractionStatus.None
            }
            current.copy(
                isFindImagesSelected = findImages,
                isDisplayImageSelected = displayImage,
                imageInteractionStatus = activeMode,
                isMarkingLocation = false
            )
        }
    }

    private fun updateInteractionState(transform: (OrientedImageryViewerUiState) -> OrientedImageryViewerUiState) {
        val next = transform(uiState)
        if (next.geoViewInteractionStatus != geoViewInteractionStatus) {
            interactionRevision++
            markers.invalidateOperations()
        }
        _uiState.value = next
        if (next.imageInteractionStatus != GeoViewInteractionStatus.DisplayImage || next.isMarkingLocation) {
            footprints.clearNoImageLocation()
        }
    }

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
            Popup(Graphic(image.geometry, image.attributes))
        }.onFailure { error ->
            if (error is CancellationException) throw error
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
     * Handles a GeoView tap according to [geoViewInteractionStatus]. Searches for images,
     * identifies the first feature in the associated oriented imagery layer and displays its image,
     * adds markers, or does nothing. An image-operation error or missing feature clears the active
     * image and displays a message in the viewer. Image-mode taps make the viewer visible.
     * Marker failures retain the active image and visibility without displaying a message.
     * Scene locations are resolved against the scene content through [geoViewProxy].
     * Taps from [LocalSceneViewProxy] are ignored without changing viewer state.
     *
     * Returns a failed [Result] if location resolution, identify, search, fetch, or marker placement fails.
     * Cancellation is rethrown. A map marker may already have been added when an operation fails.
     */
    public suspend fun onSingleTapOnGeoView(event: SingleTapConfirmedEvent): Result<Unit> {
        currentCoroutineContext().ensureActive()
        if (geoViewProxy is LocalSceneViewProxy) return Result.success(Unit)
        val status = geoViewInteractionStatus
        if (status == GeoViewInteractionStatus.None) return Result.success(Unit)
        if (status != GeoViewInteractionStatus.AddMarkers) {
            _isViewerVisible.value = true
            return withImageRequest { handleSingleTap(event, status) }
        }
        return handleSingleTap(event, status)
    }

    private suspend fun handleSingleTap(
        event: SingleTapConfirmedEvent,
        status: GeoViewInteractionStatus
    ): Result<Unit> {
        return runCatching {
            currentCoroutineContext().ensureActive()
            when (status) {
                GeoViewInteractionStatus.None -> Unit
                GeoViewInteractionStatus.DisplayImage -> {
                    val revision = ++interactionRevision
                    val result = runCatching {
                        geoViewProxy.identify(
                            layer = orientedImageryLayer,
                            screenCoordinate = event.screenCoordinate,
                            tolerance = 20.dp
                        ).getOrThrow()
                    }
                    ensureCurrentInteraction(revision)
                    val identified = result.getOrThrow()
                    identified.error?.let { throw it }
                    val feature = identified.geoElements.firstOrNull() as? Feature
                    if (feature == null) {
                        val location = when (geoViewProxy) {
                            is SceneViewProxy -> geoViewProxy.screenToLocation(event.screenCoordinate).getOrThrow()

                            else -> event.mapPoint
                        }
                        ensureCurrentInteraction(revision)
                        showMessage(R.string.no_images_found_for_tapped_location)
                        location?.let { footprints.showNoImageLocation(it) }
                    } else {
                        fetchImage(feature).getOrThrow()
                    }
                }

                GeoViewInteractionStatus.FindImages, GeoViewInteractionStatus.AddMarkers -> {
                    val revision = ++interactionRevision
                    val location = runCatching {
                        when (geoViewProxy) {
                            is SceneViewProxy -> geoViewProxy.screenToLocation(event.screenCoordinate).getOrThrow()
                            else -> event.mapPoint
                        }
                    }
                    ensureCurrentInteraction(revision)
                    val mapPoint = location.getOrThrow()
                        ?: throw IllegalStateException("No map location was found for the selected point.")
                    if (status == GeoViewInteractionStatus.FindImages) {
                        searchImages(mapPoint).getOrThrow()
                    } else {
                        markers.addFromMap(mapPoint)
                    }
                }
            }
            currentCoroutineContext().ensureActive()
        }.onFailure { error ->
            if (error is CancellationException) throw error
            currentCoroutineContext().ensureActive()
            if (status == GeoViewInteractionStatus.FindImages || status == GeoViewInteractionStatus.DisplayImage) {
                val resourceId = if (status == GeoViewInteractionStatus.FindImages) R.string.image_search_failed
                    else R.string.image_fetch_failed
                if (uiState.messageResourceId != resourceId || uiState.message != error.message) {
                    showMessage(resourceId, error.message)
                }
            }
        }
    }

    /**
     * Clears the image session and displays an error or warning message in the viewer.
     * Invalidates pending image and marker operations, retaining the toolbar's user preferences.
     * A new image request displays progress; a reset restores the initial guidance.
     */
    public fun showMessage(message: String) {
        resetImageState()
        clearOverlayChecksIfNoImage()
        updateUiState { it.copy(message = message) }
    }

    private fun showMessage(resourceId: Int, details: String? = null) {
        resetImageState()
        clearOverlayChecksIfNoImage()
        updateUiState { it.copy(messageResourceId = resourceId, message = details) }
    }

    private suspend fun ensureCurrentInteraction(revision: Long) {
        currentCoroutineContext().ensureActive()
        if (revision != interactionRevision) throw CancellationException("GeoView interaction was superseded.")
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
     * If no images are found, the active image is cleared and a warning message is displayed.
     *
     * Returns a failed [Result] if searching or loading fails and displays the failure in the viewer.
     * Cancellation is rethrown. The active image may already have been changed when an operation fails.
     */
    public suspend fun searchImages(lastMapLocation: Point): Result<ImageSearchStatus> {
        return withImageRequest {
            setInputLocation(lastMapLocation)
            session.search(lastMapLocation).also { result ->
                result.onFailure { showMessage(R.string.image_search_failed, it.message) }
                if (result.getOrNull() == ImageSearchStatus.NoImagesFound) {
                    showMessage(R.string.no_images_found_for_tapped_location)
                }
                clearOverlayChecksIfNoImage()
            }
        }
    }

    /**
     * Fetches the oriented image for the specified feature and sets it as the active image.
     *
     * Returns a failed [Result] if fetching or loading fails and displays the failure in the viewer.
     * Cancellation is rethrown. The active image may already have been changed when an operation fails.
     */
    public suspend fun fetchImage(feature: Feature): Result<Unit> {
        return withImageRequest {
            session.fetch(feature).also { result ->
                result.onFailure { showMessage(R.string.image_fetch_failed, it.message) }
                clearOverlayChecksIfNoImage()
            }
        }
    }

    private suspend fun <T> withImageRequest(operation: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        resetImageState(isLoading = true)
        val revision = imageRequestRevision
        return try {
            operation()
        } finally {
            // A nested fetch/search, replacement request, or reset owns its own loading state.
            if (revision == imageRequestRevision) {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    private fun clearOverlayChecksIfNoImage() {
        if (!hasActiveImage) {
            updateUiState {
                it.copy(
                    isActiveFootprintVisible = false,
                    isAdditionalFootprintsVisible = false,
                    isAdditionalCameraLocationsVisible = false
                )
            }
        }
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

    internal suspend fun navigateFirstImage(): Result<ImageNavigationStatus> =
        session.navigateToBoundary(last = false) { uiState.isSequentialNavigationEnabled }

    internal suspend fun navigateLastImage(): Result<ImageNavigationStatus> =
        session.navigateToBoundary(last = true) { uiState.isSequentialNavigationEnabled }

    /**
     * Toggles the visibility of the active footprint.
     */
    public fun toggleActiveFootprint() {
        val shouldShow = !uiState.isActiveFootprintVisible
        preferredActiveFootprintVisible = shouldShow
        updateUiState { uiState -> uiState.copy(isActiveFootprintVisible = shouldShow) }

        footprints.setActiveVisible(shouldShow)
    }

    /**
     * Toggles the visibility of the additional footprints.
     */
    public fun toggleAdditionalFootprints() {
        val shouldShow = !uiState.isAdditionalFootprintsVisible
        preferredAdditionalFootprintsVisible = shouldShow
        updateUiState { uiState -> uiState.copy(isAdditionalFootprintsVisible = shouldShow) }

        footprints.setAdditionalFootprintsVisible(shouldShow)
    }

    /**
     * Toggles the visibility of the additional camera locations.
     */
    public fun toggleAdditionalCameraLocations() {
        val shouldShow = !uiState.isAdditionalCameraLocationsVisible
        preferredAdditionalCameraLocationsVisible = shouldShow
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
     * Toggles marker placement, clearing image selections when enabled and returning to
     * [GeoViewInteractionStatus.None] when disabled.
     */
    public fun addMarker() {
        geoViewInteractionStatus = if (isMarkingLocation) {
            GeoViewInteractionStatus.None
        } else {
            GeoViewInteractionStatus.AddMarkers
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
        resetImageState()
        val defaults = OrientedImageryViewerUiState()
        preferredActiveFootprintVisible = defaults.isActiveFootprintVisible
        preferredAdditionalFootprintsVisible = defaults.isAdditionalFootprintsVisible
        preferredAdditionalCameraLocationsVisible = defaults.isAdditionalCameraLocationsVisible
        updateInteractionState {
            it.copy(
                isActiveFootprintVisible = preferredActiveFootprintVisible,
                isAdditionalFootprintsVisible = preferredAdditionalFootprintsVisible,
                isAdditionalCameraLocationsVisible = preferredAdditionalCameraLocationsVisible,
                isFindImagesSelected = true,
                isDisplayImageSelected = false,
                isSequentialNavigationEnabled = false,
                imageInteractionStatus = GeoViewInteractionStatus.FindImages,
                isMarkingLocation = false
            )
        }
    }

    /** Hides and resets the viewer, invalidating pending image and marker operations. */
    public fun dismissViewer() {
        resetAll()
        _isViewerVisible.value = false
    }

    private fun resetImageState(isLoading: Boolean = false) {
        interactionRevision++
        imageRequestRevision++
        footprints.reset()
        session.reset()
        markers.reset()
        updateUiState { uiState ->
            uiState.copy(
                activeImageInfo = null,
                isLoading = isLoading,
                inputLocationData = null,
                message = null,
                messageResourceId = null
            )
        }
    }

    /**
     * Sets whether sequential navigation is enabled for the viewer.
     * Enabling clears the input location and additional images. Disabling retains the currently
     * displayed image as the sole result, without restoring the previous search context.
     * Mode changes invalidate pending navigation requests.
     */
    public fun setSequentialNavigationEnabled(enabled: Boolean) {
        if (enabled == uiState.isSequentialNavigationEnabled) return
        updateUiState { uiState -> uiState.copy(isSequentialNavigationEnabled = enabled) }
        session.navigationModeChanged()
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
                message = if (orientedImage != null) null else uiState.message,
                messageResourceId = if (orientedImage != null) null else uiState.messageResourceId,
                // Both additional overlays are built lazily from these images when selected.
                hasAdditionalImages = orientedImage != null && session.additionalImages.isNotEmpty(),
                activeImageRevision = uiState.activeImageRevision + 1,
                imageSessionRevision = uiState.imageSessionRevision + if (orientedImage == null) 1 else 0,
                searchResultCount = session.images.size,
                selectedResultIndex = session.activeIndex,
                isActiveFootprintVisible = if (orientedImage != null) {
                    preferredActiveFootprintVisible
                } else uiState.isActiveFootprintVisible,
                isAdditionalFootprintsVisible = if (orientedImage != null) {
                    preferredAdditionalFootprintsVisible
                } else uiState.isAdditionalFootprintsVisible,
                isAdditionalCameraLocationsVisible = if (orientedImage != null) {
                    preferredAdditionalCameraLocationsVisible
                } else uiState.isAdditionalCameraLocationsVisible
            )
        }
    }
}
