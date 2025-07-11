/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.offline.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/**
 * Typography styling properties for the OfflineMapAreas composable.
 *
 * @param title The text style for the title.
 * @param layerName The text style for the layer name.
 * @param subLayerName The text style for the sub layer name.
 * @param legendInfoName The text style for the legend info name.
 *
 * @since 200.8.0
 */
@Immutable
@ExposedCopyVisibility
public data class Typography internal constructor(
    val offlineMapAreasTitle: TextStyle,
    val preplannedMapAreaTitle: TextStyle,
    val preplannedMapAreaDescription: TextStyle,
    val preplannedMapAreaStatus: TextStyle,
    val mapAreasDetailsTitle: TextStyle,
    val mapAreasDetailsSize: TextStyle,
    val mapAreasDetailsDescriptionLabel: TextStyle,
    val mapAreasDetailsDescription: TextStyle,

    val onDemandMapAreasTitle: TextStyle,

)

/**
 * Default values for the OfflineMapAreas composable.
 *
 * @since 200.8.0
 */
public object OfflineMapAreasDefaults {

    /**
     * Creates default typography values for the OfflineMapAreas composable.
     *
     * @param title The text style for the title.
     * @param layerName The text style for the layer name.
     * @param subLayerName The text style for the sub layer name.
     * @param legendInfoName The text style for the legend info name.
     * @since 200.8.0
     */
    @Composable
    public fun typography(
        title: TextStyle = MaterialTheme.typography.titleLarge,
        layerName: TextStyle = MaterialTheme.typography.titleSmall,
        subLayerName: TextStyle = MaterialTheme.typography.bodyMedium,
        legendInfoName: TextStyle = MaterialTheme.typography.bodyMedium,
        ): Typography = Typography(
        title = title,
        layerName = layerName,
        subLayerName = subLayerName,
        legendInfoName = legendInfoName,
    )
}



