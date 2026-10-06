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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerController
import com.arcgismaps.toolkit.orientedimageryviewer.internal.OrientedImageryViewerUiState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.PointData
import kotlinx.coroutines.CoroutineScope

@Immutable
public sealed interface SearchImagesOutcome {
    @Immutable
    public data class ImagesFound(
        public val count: Int
    ) : SearchImagesOutcome

    @Immutable
    public object NoImagesFound : SearchImagesOutcome
}

@Immutable
public sealed interface NavigationOutcome {
    @Immutable
    public object Moved : NavigationOutcome

    @Immutable
    public object NoActiveImage : NavigationOutcome

    @Immutable
    public object BoundaryReached : NavigationOutcome
}

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

    private val _uiState: MutableState<OrientedImageryViewerUiState> = mutableStateOf(OrientedImageryViewerUiState())
    private val controller = OrientedImageryViewerController(orientedImageryLayer, graphicsOverlay, coroutineScope, this)

    internal val uiState: OrientedImageryViewerUiState
        get() = _uiState.value

    public val supportsSequentialNavigation: Boolean = orientedImageryLayer.supportsSequentialNavigation

    internal val inputLocationData: PointData?
        get() = uiState.inputLocationData

    public val isSequentialNavigationEnabled: Boolean
        get() = uiState.isSequentialNavigationEnabled

    public val hasActiveImage: Boolean
        get() = uiState.activeImageInfo != null

    internal fun updateUiState(transform: (OrientedImageryViewerUiState) -> OrientedImageryViewerUiState) {
        _uiState.value = transform(_uiState.value)
    }


    internal suspend fun imageToLocation(imagePoint: DoubleXY): Result<Point>? = controller.imageToLocation(imagePoint)

    internal suspend fun locationToImage(mapPoint: Point): Result<DoubleXY>? = controller.locationToImage(mapPoint)

    internal fun setInputLocation(point: Point?) {
        controller.setInputLocation(point)
    }

    public suspend fun searchImages(lastMapLocation: Point): Result<SearchImagesOutcome> = controller.searchImages(lastMapLocation)

    public suspend fun fetchImage(feature: Feature): Result<Unit> = controller.fetchImage(feature)

    public suspend fun navigateNextImage(): Result<NavigationOutcome> = controller.navigateNextImage()

    public suspend fun navigatePreviousImage(): Result<NavigationOutcome> = controller.navigatePreviousImage()

    public fun toggleActiveFootprint() {
        controller.toggleActiveFootprint()
    }

    public fun toggleAdditionalFootprints() {
        controller.toggleAdditionalFootprints()
    }

    public fun toggleAdditionalCameraLocations() {
        controller.toggleAdditionalCameraLocations()
    }

    public fun toggleSequentialNavigation() {
        controller.toggleSequentialNavigation()
    }

    internal fun updateFootprints(imageCoordinates: List<DoubleXY>) {
        controller.updateFootprints(imageCoordinates)
    }

    public fun isActiveFootprintVisible(): Boolean = controller.isActiveFootprintVisible()

    public fun resetAll() {
        controller.resetAll()
    }

    /**
     * Sets whether sequential navigation is enabled for the viewer.
     */
    public fun setSequentialNavigationEnabled(enabled: Boolean) {
        controller.setSequentialNavigationEnabled(enabled)
    }
}


