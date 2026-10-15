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

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import androidx.compose.runtime.Immutable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState

/**
 * Immutable snapshot of the UI-facing state for [OrientedImageryViewerState].
 */
@Immutable
internal data class OrientedImageryViewerUiState(
    val activeImageInfo: OrientedImageInfo? = null,
    val inputLocationData: PointData? = null,
    val isSequentialNavigationEnabled: Boolean = false,
    val isActiveFootprintVisible: Boolean = false,
    val isAdditionalFootprintsVisible: Boolean = false,
    val isAdditionalCameraLocationsVisible: Boolean = false
)

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
    internal fun toPoint(): Point = if (z == null) {
        spatialReferenceWkid?.let { wkid -> Point(x, y, SpatialReference(wkid)) } ?: Point(x, y)
    } else {
        spatialReferenceWkid?.let { wkid -> Point(x, y, z, SpatialReference(wkid)) } ?: Point(x, y, z)
    }
}




