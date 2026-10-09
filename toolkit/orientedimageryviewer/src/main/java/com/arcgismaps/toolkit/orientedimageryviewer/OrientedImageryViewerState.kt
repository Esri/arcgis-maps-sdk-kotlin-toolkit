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
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ImageDefaults
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageInfo
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerUiState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PointData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private var activeIndex = -1
    private var additionalCameraLocationGraphics: List<Graphic> = emptyList()
    private var additionalFootprints: List<OrientedImageFootprint> = emptyList()
    private var footprintUpdateJob: Job? = null
    private var pendingFootprintCoordinates: List<DoubleXY>? = null

    internal val uiState: OrientedImageryViewerUiState
        get() = _uiState.value

    public val supportsSequentialNavigation: Boolean =
        orientedImageryLayer.supportsSequentialNavigation

    internal val inputLocationData: PointData?
        get() = uiState.inputLocationData

    public val isSequentialNavigationEnabled: Boolean
        get() = uiState.isSequentialNavigationEnabled

    public val hasActiveImage: Boolean
        get() = uiState.activeImageInfo != null

    private fun updateUiState(transform: (OrientedImageryViewerUiState) -> OrientedImageryViewerUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun inputLocation(): Point? = uiState.inputLocationData?.toPoint()

    internal suspend fun imageToLocation(imagePoint: DoubleXY): Result<Point>? =
        activeOrientedImage?.imageToLocation(imagePoint)

    internal suspend fun locationToImage(mapPoint: Point): Result<DoubleXY>? =
        activeOrientedImage?.locationToImage(mapPoint)

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

    public suspend fun searchImages(lastMapLocation: Point): Result<ImageSearchStatus> {
        resetAll()
        Log.i("OrientedImagery", "Searching for oriented images at map location: ${lastMapLocation.toJson()}")
        setInputLocation(lastMapLocation)
        return orientedImageryLayer.searchImages(
            lastMapLocation,
            OrientedImageSearchParameters().apply { applyHeightFilter = false }
        ).fold(
            onSuccess = { results ->
                Log.i("OrientedImagery", "Found ${results.size} oriented images at location: $lastMapLocation")
                if (results.isEmpty()) {
                    Log.i("OrientedImagery", "No oriented images found at location: $lastMapLocation")
                    Result.success(ImageSearchStatus.NoImagesFound)
                } else {
                    orientedImages = results
                    val orientedImage = results[0]
                    orientedImage.load().fold(
                        onSuccess = {
                            setActiveOrientedImage(orientedImage)
                            activeIndex = 0
                            Log.i("OrientedImagery", "Image URI: ${activeOrientedImage!!.dataUri}")
                            Result.success(ImageSearchStatus.ImagesFound(results.size))
                        },
                        onFailure = { error ->
                            setActiveOrientedImage(null)
                            activeIndex = -1
                            Log.i(
                                "OrientedImagery",
                                "Failed to load oriented image: ${error.message}; URI: ${orientedImage.dataUri}; Original URI: ${orientedImage.attributes["ImagePath"]}"
                            )
                            Result.failure(error)
                        }
                    )
                }
            },
            onFailure = { error ->
                setActiveOrientedImage(null)
                activeIndex = -1
                Log.i("OrientedImagery", "Failed to search for oriented images: ${error.message}")
                Result.failure(error)
            }
        )
    }

    public suspend fun fetchImage(feature: Feature): Result<Unit> {
        resetAll()
        Log.i("OrientedImagery", "Fetching image for feature: ObjectID = ${feature.attributes["ObjectID"]}")
        return orientedImageryLayer.fetchImageForFeature(feature).fold(
            onSuccess = { orientedImage ->
                orientedImage.load().fold(
                    onSuccess = {
                        setInputLocation(feature.geometry as? Point)
                        setActiveOrientedImage(orientedImage)
                        orientedImages = listOf(orientedImage)
                        activeIndex = 0
                        Log.i("OrientedImagery", "Image URI: ${activeOrientedImage!!.dataUri}")
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        setActiveOrientedImage(null)
                        activeIndex = -1
                        Log.i(
                            "OrientedImagery",
                            "Failed to load oriented image: ${error.message}; URI: ${orientedImage.dataUri}; Original URI: ${orientedImage.attributes["ImagePath"]}"
                        )
                        Result.failure(error)
                    }
                )
            },
            onFailure = { error ->
                Log.i("OrientedImagery", "Failed to fetch oriented image for feature: $error")
                Result.failure(error)
            }
        )
    }

    public suspend fun navigateNextImage(): Result<ImageNavigationStatus> {
        if (!hasActiveImage) {
            return Result.success(ImageNavigationStatus.NoActiveImage)
        }
        return if (uiState.isSequentialNavigationEnabled) {
            fetchAdjacentImage(SequenceStep.Next)
        } else {
            showNextImageInList()
        }
    }

    public suspend fun navigatePreviousImage(): Result<ImageNavigationStatus> {
        if (!hasActiveImage) {
            return Result.success(ImageNavigationStatus.NoActiveImage)
        }
        return if (uiState.isSequentialNavigationEnabled) {
            fetchAdjacentImage(SequenceStep.Previous)
        } else {
            showPreviousImageInList()
        }
    }

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

    public fun toggleAdditionalFootprints() {
        val shouldShow = !uiState.isAdditionalFootprintsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalFootprintsVisible = shouldShow) }

        if (shouldShow) {
            showAdditionalFootprints()
        } else {
            orientedImageryLayer.visibleFootprints.removeAll(additionalFootprints)
        }
    }

    public fun toggleAdditionalCameraLocations() {
        val shouldShow = !uiState.isAdditionalCameraLocationsVisible
        updateUiState { uiState -> uiState.copy(isAdditionalCameraLocationsVisible = shouldShow) }

        if (shouldShow) {
            showAdditionalCameraLocations()
        } else {
            graphicsOverlay.graphics.removeAll(additionalCameraLocationGraphics)
        }
    }

    public fun toggleSequentialNavigation() {
        setSequentialNavigationEnabled(!uiState.isSequentialNavigationEnabled)
    }

    internal fun updateFootprints(imageCoordinates: List<DoubleXY>) {
        if (!uiState.isActiveFootprintVisible) return

        activeFootprint?.let { footprint ->
            pendingFootprintCoordinates = imageCoordinates
            if (footprintUpdateJob?.isActive == true) {
                return
            }

            footprintUpdateJob = coroutineScope.launch {
                while (true) {
                    val coordinatesToUpdate = pendingFootprintCoordinates ?: break
                    pendingFootprintCoordinates = null

                    footprint.updateFootprint(coordinatesToUpdate).onSuccess {
                        Log.i(
                            "OrientedImageryViewerState",
                            "Footprint updated successfully for image: ${footprint.orientedImage.dataUri}"
                        )
                    }.onFailure {
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

    public fun isActiveFootprintVisible(): Boolean = uiState.isActiveFootprintVisible

    public fun resetAll() {
        setActiveOrientedImage(null)
        activeFootprint = null
        activeIndex = -1
        activeCameraLocationGraphic = null
        setInputLocation(null)
        updateUiState { uiState ->
            uiState.copy(
                isActiveFootprintVisible = false,
                isAdditionalFootprintsVisible = false,
                isAdditionalCameraLocationsVisible = false,
                activeImageInfo = null,
                inputLocationData = null
            )
        }
        cancelFootprintUpdateJob()
        orientedImageryLayer.visibleFootprints.clear()
        graphicsOverlay.graphics.clear()
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
                type = image.type
            )
        })
    }

    private fun setActiveImageInfo(value: OrientedImageInfo?) {
        updateUiState { uiState -> uiState.copy(activeImageInfo = value) }
    }

    private fun setInputLocationData(value: PointData?) {
        updateUiState { uiState -> uiState.copy(inputLocationData = value) }
    }

    private suspend fun fetchAdjacentImage(step: SequenceStep): Result<ImageNavigationStatus> {
        val currentImage = activeOrientedImage ?: return Result.success(ImageNavigationStatus.NoActiveImage)
        Log.i(
            "OrientedImagery",
            "Fetching adjacent image for ${orientedImageryLayer.name} with supportSequentialNavigation = ${orientedImageryLayer.supportsSequentialNavigation}"
        )
        return orientedImageryLayer.fetchAdjacentImage(currentImage, step).fold(
            onSuccess = { adjacentImage ->
                adjacentImage.load().fold(
                    onSuccess = {
                        setActiveOrientedImage(adjacentImage)
                        resetActive()
                        Log.i("OrientedImagery", "Switched to ${step.javaClass.simpleName} image: ${adjacentImage.dataUri}")
                        Result.success(ImageNavigationStatus.Moved)
                    },
                    onFailure = { error ->
                        Log.i("OrientedImagery", "Failed to load ${step.javaClass.simpleName} image: ${error.message}")
                        Result.failure(error)
                    }
                )
            },
            onFailure = { error ->
                Log.i("OrientedImagery", "Failed to fetch ${step.javaClass.simpleName} image: ${error.message}")
                Result.failure(error)
            }
        )
    }

    private suspend fun showNextImageInList(): Result<ImageNavigationStatus> {
        if (orientedImages.isEmpty() || (activeIndex + 1 >= orientedImages.size)) {
            return Result.success(ImageNavigationStatus.BoundaryReached)
        }
        val nextImage = orientedImages[activeIndex + 1]
        return nextImage.load().fold(
            onSuccess = {
                setActiveOrientedImage(nextImage)
                ++activeIndex
                resetActive()
                Log.i("OrientedImagery", "Switched to next image in image result: ${nextImage.dataUri}")
                Result.success(ImageNavigationStatus.Moved)
            },
            onFailure = { error ->
                Log.i("OrientedImagery", "Failed to load next image in image result: ${error.message}")
                Result.failure(error)
            }
        )
    }

    private suspend fun showPreviousImageInList(): Result<ImageNavigationStatus> {
        if (orientedImages.isEmpty() || (activeIndex - 1 < 0)) {
            return Result.success(ImageNavigationStatus.BoundaryReached)
        }
        val previousImage = orientedImages[activeIndex - 1]
        return previousImage.load().fold(
            onSuccess = {
                setActiveOrientedImage(previousImage)
                --activeIndex
                resetActive()
                Log.i("OrientedImagery", "Switched to previous image in image result: ${previousImage.dataUri}")
                Result.success(ImageNavigationStatus.Moved)
            },
            onFailure = { error ->
                Log.i("OrientedImagery", "Failed to load previous image in image result: ${error.message}")
                Result.failure(error)
            }
        )
    }

    private fun showActiveFootprint() {
        if (activeFootprint == null) {
            activeFootprint = getOrCreateActiveFootprint()
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
        pendingFootprintCoordinates = null
    }

    private fun getOrCreateActiveFootprint(): OrientedImageFootprint {
        val activeImage = requireNotNull(activeOrientedImage) {
            "An active oriented image is required to create a footprint."
        }

        return orientedImageryLayer.visibleFootprints
            .firstOrNull { footprint -> footprint.orientedImage.dataUri == activeImage.dataUri }
            ?: OrientedImageFootprint(activeImage).also { footprint ->
                Log.i("OrientedImageryViewerState", "Creating new footprint for active image: ${activeImage.dataUri}")
                footprint.outlineColor = Color.transparent
                orientedImageryLayer.visibleFootprints.add(footprint)

                if (uiState.isSequentialNavigationEnabled) {
                    graphicsOverlay.graphics.clear()
                } else {
                    inputLocation()?.let { inputLoc ->
                        val graphicInputLocation = Graphic(inputLoc, ImageDefaults.inputLocationSymbol())
                        graphicsOverlay.graphics.add(graphicInputLocation)
                    }
                }
                activeCameraLocationGraphic = Graphic(activeImage.geometry, ImageDefaults.activeCameraLocationSymbol())
                graphicsOverlay.graphics.add(activeCameraLocationGraphic!!)
            }
    }
}


