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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Immutable
internal data class OrientedImageInfo(
    val dataUri: String?,
    val type: OrientedImageType?
)

@Immutable
internal data class PointData(
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val spatialReferenceWkid: Int? = null
) {
    fun toPoint(): Point = if (z == null) {
        spatialReferenceWkid?.let { wkid -> Point(x, y, SpatialReference(wkid)) } ?: Point(x, y)
    } else {
        spatialReferenceWkid?.let { wkid -> Point(x, y, z, SpatialReference(wkid)) } ?: Point(x, y, z)
    }
}

/**
 * The state object for an [OrientedImageryLayer] used by the Oriented Imagery Viewer composable.
 * Hoist this state out of the composition to ensure that viewer state is not lost during
 * configuration changes.
 *
 * @param orientedImageryLayer The oriented imagery layer displayed by the viewer.
 * @param graphicsOverlay A [GraphicsOverlay] used to display camera location and input location graphics.
 * @param coroutineScope A [CoroutineScope] used for asynchronous layer and image operations.
 *
 * @since 300.2.0
 */
@Stable
public class OrientedImageryViewerState public constructor(
    internal val orientedImageryLayer: OrientedImageryLayer,
    private val graphicsOverlay: GraphicsOverlay,
    internal val coroutineScope: CoroutineScope
) {
    /**
     * The title to display for the viewer.
     */
    public val title: String = orientedImageryLayer.name.ifBlank { "Oriented imagery" }

    internal var inputLocationData: PointData? by mutableStateOf(null)

    private var orientedImages: List<OrientedImage> = emptyList()

    internal var activeImageInfo: OrientedImageInfo? by mutableStateOf(null)
    private var activeOrientedImage: OrientedImage? = null
    private var activeFootprint: OrientedImageFootprint? = null
    private var activeCameraLocationGraphic : Graphic? = null
    private var activeIndex = -1

    private var additionalCameraLocationGraphics : List<Graphic> = emptyList()
    private var additionalFootprints: List<OrientedImageFootprint> = emptyList()

    private var isActiveFootprintVisible: Boolean = false
    private var isAdditionalFootprintsVisible: Boolean = false
    private var isAdditionalCameraLocationsVisible: Boolean = false

    public var isSequentialNavigationEnabled: Boolean by mutableStateOf(false)
    public val supportsSequentialNavigation: Boolean = orientedImageryLayer.supportsSequentialNavigation
    public val hasActiveImage: Boolean
        get() = activeImageInfo != null

    private var footprintUpdateJob: Job? = null
    private var pendingFootprintCoordinates: List<DoubleXY>? = null

    internal fun inputLocation(): Point? = inputLocationData?.toPoint()

    internal suspend fun imageToLocation(imagePoint: DoubleXY): Result<Point>? = activeOrientedImage?.imageToLocation(imagePoint)

    internal suspend fun locationToImage(mapPoint: Point): Result<DoubleXY>? = activeOrientedImage?.locationToImage(mapPoint)

    private fun setActiveOrientedImage(orientedImage: OrientedImage?) {
        activeOrientedImage = orientedImage
        activeImageInfo = orientedImage?.toImageInfo()
    }

    internal fun setInputLocation(point: Point?) {
        inputLocationData = point?.toPointData()
    }

    public fun searchImages(lastMapLocation: Point) {
        resetAll()
        coroutineScope.launch {
            Log.i("OrientedImagery", "Searching for oriented images at map location: ${lastMapLocation.toJson()}")
            setInputLocation(lastMapLocation)
            orientedImageryLayer.searchImages(lastMapLocation, OrientedImageSearchParameters().apply { applyHeightFilter = false }).onSuccess { results ->
                Log.i("OrientedImagery", "Found ${results.size} oriented images at location: $lastMapLocation")
                if (results.isEmpty()) {
                    Log.i("OrientedImagery", "No oriented images found at location: $lastMapLocation")
                    return@onSuccess
                }
                orientedImages = results
                val orientedImage = results[0]
                orientedImage.load().onSuccess {
                    setActiveOrientedImage(orientedImage)
                    activeIndex = 0
                    Log.i("OrientedImagery", "Image URI: ${activeOrientedImage!!.dataUri}")
                }.onFailure { error ->
                    setActiveOrientedImage(null)
                    activeIndex = -1
                    Log.i("OrientedImagery", "Failed to load oriented image: ${error.message}; URI: ${orientedImage.dataUri}; Original URI: ${orientedImage.attributes["ImagePath"]}")
                }
            }.onFailure { error ->
                setActiveOrientedImage(null)
                activeIndex = -1
                Log.i("OrientedImagery", "Failed to search for oriented images: ${error.message}")
            }
        }
    }

    public fun fetchImage(feature: Feature) {
        resetAll()
        coroutineScope.launch {
            Log.i("OrientedImagery", "Fetching image for feature: ObjectID = ${feature.attributes["ObjectID"]}")
            orientedImageryLayer.fetchImageForFeature(feature).onSuccess { orientedImage ->
                orientedImage.load().onSuccess {
                    setInputLocation(feature.geometry as? Point)
                    setActiveOrientedImage(orientedImage)
                    orientedImages = listOf(orientedImage)
                    activeIndex = 0
                    Log.i("OrientedImagery", "Image URI: ${activeOrientedImage!!.dataUri}")
                }.onFailure { error ->
                    Log.i("OrientedImagery", "Failed to load oriented image: ${error.message}; URI: ${orientedImage.dataUri}; Original URI: ${orientedImage.attributes["ImagePath"]}")
                }
            }.onFailure { error ->
                Log.i("OrientedImagery", "Failed to fetch oriented image for feature: $error")
            }
        }
    }

    public fun showNextImage() {
        if (isSequentialNavigationEnabled) {
            fetchAdjacentImage(SequenceStep.Next)
        } else {
            showNextImageInList()
        }
    }

    private fun fetchAdjacentImage(step: SequenceStep) {
        activeOrientedImage?.let { currentImage ->
            coroutineScope.launch {
                Log.i("OrientedImagery", "Fetching adjacent image for ${orientedImageryLayer.name} with supportSequentialNavigation = ${supportsSequentialNavigation}")
                orientedImageryLayer.fetchAdjacentImage(currentImage, step).onSuccess { adjacentImage ->
                    adjacentImage.load().onSuccess {
                        setActiveOrientedImage(adjacentImage)
                        resetActive()
                        Log.i("OrientedImagery", "Switched to ${step.javaClass.simpleName} image: ${adjacentImage.dataUri}")
                    }.onFailure { error ->
                        Log.i("OrientedImagery", "Failed to load ${step.javaClass.simpleName} image: ${error.message}")
                    }
                }.onFailure { error ->
                    Log.i("OrientedImagery", "Failed to fetch ${step.javaClass.simpleName} image: ${error.message}")
                }
            }
        }
    }

    private fun showNextImageInList() {
        if (orientedImages.isEmpty() || (activeIndex + 1 >= orientedImages.size)) return
        val nextImage = orientedImages[activeIndex + 1]
        coroutineScope.launch {
            nextImage.load().onSuccess {
                setActiveOrientedImage(nextImage)
                ++activeIndex
                resetActive()
                Log.i("OrientedImagery", "Switched to next image in image result: ${nextImage.dataUri}")
            }.onFailure { error ->
                Log.i("OrientedImagery", "Failed to load next image in image result: ${error.message}")
            }
        }
    }

    public fun showPreviousImage() {
        if (isSequentialNavigationEnabled) {
            fetchAdjacentImage(SequenceStep.Previous)
        } else {
            showPreviousImageInList()
        }
    }

    private fun showPreviousImageInList() {
        if (orientedImages.isEmpty() || (activeIndex - 1 < 0)) return
        val previousImage = orientedImages[activeIndex - 1]
        coroutineScope.launch {
            previousImage.load().onSuccess {
                setActiveOrientedImage(previousImage)
                --activeIndex
                resetActive()
                Log.i("OrientedImagery", "Switched to previous image in image result: ${previousImage.dataUri}")
            }.onFailure { error ->
                Log.i("OrientedImagery", "Failed to load previous image in image result: ${error.message}")
            }
        }
    }

    public fun toggleActiveFootprint() {
        isActiveFootprintVisible = !isActiveFootprintVisible
        if (isActiveFootprintVisible) {
            showActiveFootprint()
        } else {
            activeFootprint?.let { footprint ->
                orientedImageryLayer.visibleFootprints.remove(footprint)
            }
            activeCameraLocationGraphic?.isVisible = false
        }
    }

    private fun showActiveFootprint() {
        if (activeFootprint == null) {
            activeFootprint = getOrCreateActiveFootprint()
        } else {
            orientedImageryLayer.visibleFootprints.add(activeFootprint!!)
            activeCameraLocationGraphic?.isVisible = true
        }
    }

    public fun toggleAdditionalFootprints() {
        isAdditionalFootprintsVisible = !isAdditionalFootprintsVisible
        if (isAdditionalFootprintsVisible) {
            showAdditionalFootprints()
        } else {
            orientedImageryLayer.visibleFootprints.removeAll(additionalFootprints)
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

    public fun toggleAdditionalCameraLocations() {
        isAdditionalCameraLocationsVisible = !isAdditionalCameraLocationsVisible
        if (isAdditionalCameraLocationsVisible) {
            showAdditionalCameraLocations()
        } else {
            graphicsOverlay.graphics.removeAll(additionalCameraLocationGraphics)
        }
    }

    public fun toggleSequentialNavigation() {
        isSequentialNavigationEnabled = !isSequentialNavigationEnabled
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

    public fun updateFootprints(imageCoordinates: List<DoubleXY>) {
        if (!isActiveFootprintVisible) return

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

    public fun isActiveFootprintVisible(): Boolean = isActiveFootprintVisible

    /**
     * Resets the viewer state, clearing the active oriented image, its associated footprint and camera location graphics,
     * and any additional footprints or camera location graphics. This method is typically called when switching to a new
     * active oriented image or when resetting the viewer.
     */
    public fun resetAll() {
        setActiveOrientedImage(null)
        activeFootprint = null
        activeIndex = -1
        activeCameraLocationGraphic = null
        setInputLocation(null)
        isActiveFootprintVisible = false
        isAdditionalFootprintsVisible = false
        isAdditionalCameraLocationsVisible = false
        cancelFootprintUpdateJob()
        orientedImageryLayer.visibleFootprints.clear()
        graphicsOverlay.graphics.clear()
        orientedImages = emptyList()
        additionalFootprints = emptyList()
        additionalCameraLocationGraphics = emptyList()
    }

    /**
     * Resets the active oriented image and its associated footprint and camera location graphics.
     * This method is called when switching to a new active oriented image, ensuring that the
     * previous image's footprint and graphics are removed from the viewer.
     */
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

        if (isActiveFootprintVisible) showActiveFootprint()
        if (isAdditionalFootprintsVisible) showAdditionalFootprints()
        if (isAdditionalCameraLocationsVisible) showAdditionalCameraLocations()
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

                // Add graphics for camera location and input location
                if (isSequentialNavigationEnabled) {
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

private fun OrientedImage.toImageInfo(): OrientedImageInfo = OrientedImageInfo(
    dataUri = dataUri,
    type = type
)

private fun Point.toPointData(): PointData = PointData(
    x = x,
    y = y,
    z = z,
    spatialReferenceWkid = spatialReference?.wkid
)

