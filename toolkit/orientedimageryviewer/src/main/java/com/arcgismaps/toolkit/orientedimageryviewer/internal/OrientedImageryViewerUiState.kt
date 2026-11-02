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

import androidx.compose.runtime.Immutable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.toolkit.orientedimageryviewer.GeoViewInteractionStatus
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState

/**
 * Immutable snapshot of the UI-facing state for [OrientedImageryViewerState].
 */
@Immutable
internal data class OrientedImageryViewerUiState(
    val activeImageInfo: OrientedImageInfo? = null,
    val hasAdditionalImages: Boolean = false,
    val activeImageRevision: Long = 0,
    val imageSessionRevision: Long = 0,
    val searchResultCount: Int = 0,
    val selectedResultIndex: Int = -1,
    val noImagesFoundRevision: Long? = null,
    val inputLocationData: PointData? = null,
    val isSequentialNavigationEnabled: Boolean = false,
    val canNavigatePrevious: Boolean = false,
    val canNavigateNext: Boolean = false,
    val previousImageError: String? = null,
    val nextImageError: String? = null,
    val isFindImagesSelected: Boolean = true,
    val isDisplayImageSelected: Boolean = false,
    val imageInteractionStatus: GeoViewInteractionStatus = GeoViewInteractionStatus.FindImages,
    val isMarkingLocation: Boolean = false,
    val isActiveFootprintVisible: Boolean = true,
    val isAdditionalFootprintsVisible: Boolean = false,
    val isAdditionalCameraLocationsVisible: Boolean = true
) {
    val geoViewInteractionStatus: GeoViewInteractionStatus
        get() = if (isMarkingLocation) GeoViewInteractionStatus.AddMarkers else imageInteractionStatus
}

@Immutable
internal data class OrientedImageInfo(
    val dataUri: String?,
    val type: OrientedImageType?,
    val cameraLocationData: PointData? = null
)

@Immutable
internal data class PointData(
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val spatialReferenceJson: String? = null
) {
    internal fun toPoint(): Point {
        val reference = spatialReferenceJson?.let { json ->
            requireNotNull(SpatialReference.fromJsonOrNull(json)) { "Invalid spatial reference snapshot." }
        }
        return if (z == null) Point(x, y, reference) else Point(x, y, z, reference)
    }
}

/**
 * Converts a [Point] to a [PointData] snapshot for use in [OrientedImageryViewerUiState].
 */
internal fun Point.toPointData(): PointData = PointData(x, y, z, spatialReference?.toJson())
