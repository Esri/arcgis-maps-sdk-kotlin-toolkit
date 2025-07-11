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

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography styling properties for the OfflineMapAreas composable.
 *
 * This class defines the text styles used for various elements in the OfflineMapAreas UI,
 * including titles, descriptions, and status labels for both preplanned and on-demand map areas.
 * It provides a structured way to manage text appearance, ensuring consistency across the UI.
 *
 * @param offlineMapAreasTitle The text style for the title of the Offline Map Areas.
 * @param preplannedMapAreaTitle The text style for the title of preplanned map area.
 * @param preplannedMapAreaDescription The text style for the description of preplanned map area.
 * @param preplannedMapAreaStatus The text style for the status of preplanned map area.
 * @param mapAreasDetailsTitle The text style for the title in map areas details.
 * @param mapAreasDetailsSize The text style for the size label in map areas details.
 * @param mapAreasDetailsDescriptionLabel The text style for the description label in map areas details.
 * @param mapAreasDetailsDescription The text style for the description in map areas details.
 * @param onDemandMapAreasTitle The text style for the title of on-demand map areas.
 * @param onDemandMapAreaStatus The text style for the status of on-demand map area.
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
    val onDemandMapAreaStatus: TextStyle,

    val onDemandMapAreaSelectorTitle: TextStyle,
    val onDemandMapAreaSelectorMessage: TextStyle,
    val onDemandMapAreaSelectorAreaName: TextStyle,
    val onDemandMapAreaSelectorRenameButtonTextStyle: TextStyle,

    val areaNameDialogTitle: TextStyle
)

@Immutable
@ExposedCopyVisibility
public data class ColorScheme internal constructor(
    val downloadButtonColor: Color,
    val cancelButtonColor: Color,
    val cancelDownloadButtonColor: Color
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
     * @param offlineMapAreasTitle The text style for the title of the Offline Map Areas.
     * @param preplannedMapAreaTitle The text style for the title of preplanned map area.
     * @param preplannedMapAreaDescription The text style for the description of preplanned map area.
     * @param preplannedMapAreaStatus The text style for the status of preplanned map area.
     * @param mapAreasDetailsTitle The text style for the title in map areas details.
     * @param mapAreasDetailsSize The text style for the size label in map areas details.
     * @param mapAreasDetailsDescriptionLabel The text style for the description label in map areas details.
     * @param mapAreasDetailsDescription The text style for the description in map areas details.
     * @param onDemandMapAreasTitle The text style for the title of on-demand map areas.
     * @param onDemandMapAreaStatus The text style for the status of on-demand map area.
     *
     * @since 200.8.0
     */
    @Composable
    public fun typography(
        offlineMapAreasTitle: TextStyle = MaterialTheme.typography.titleMedium,
        preplannedMapAreaTitle: TextStyle = MaterialTheme.typography.titleSmall,
        preplannedMapAreaDescription: TextStyle = MaterialTheme.typography.bodySmall,
        preplannedMapAreaStatus: TextStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        ),
        mapAreasDetailsTitle: TextStyle = MaterialTheme.typography.titleLarge,
        mapAreasDetailsSize: TextStyle = MaterialTheme.typography.bodyMedium,
        mapAreasDetailsDescriptionLabel: TextStyle = MaterialTheme.typography.labelSmall,
        mapAreasDetailsDescription: TextStyle = MaterialTheme.typography.bodyMedium,
        onDemandMapAreasTitle: TextStyle = MaterialTheme.typography.titleSmall,
        onDemandMapAreaStatus: TextStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        ),
        onDemandMapAreaSelectorTitle: TextStyle = MaterialTheme.typography.titleMedium,
        onDemandMapAreaSelectorMessage: TextStyle = MaterialTheme.typography.labelSmall,
        onDemandMapAreaSelectorAreaName: TextStyle = MaterialTheme.typography.titleLarge,
        onDemandMapAreaSelectorRenameButtonTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
        areaNameDialogTitle: TextStyle = MaterialTheme.typography.titleLarge
    ): Typography = Typography(
        offlineMapAreasTitle = offlineMapAreasTitle,
        preplannedMapAreaTitle = preplannedMapAreaTitle,
        preplannedMapAreaDescription = preplannedMapAreaDescription,
        preplannedMapAreaStatus = preplannedMapAreaStatus,
        mapAreasDetailsTitle = mapAreasDetailsTitle,
        mapAreasDetailsSize = mapAreasDetailsSize,
        mapAreasDetailsDescriptionLabel = mapAreasDetailsDescriptionLabel,
        mapAreasDetailsDescription = mapAreasDetailsDescription,
        onDemandMapAreasTitle = onDemandMapAreasTitle,
        onDemandMapAreaStatus = onDemandMapAreaStatus,
        onDemandMapAreaSelectorTitle = onDemandMapAreaSelectorTitle,
        onDemandMapAreaSelectorMessage = onDemandMapAreaSelectorMessage,
        onDemandMapAreaSelectorAreaName = onDemandMapAreaSelectorAreaName,
        onDemandMapAreaSelectorRenameButtonTextStyle = onDemandMapAreaSelectorRenameButtonTextStyle,
        areaNameDialogTitle = areaNameDialogTitle
    )

    @Composable
    public fun colorScheme(
        downloadButtonColor: Color = MaterialTheme.colorScheme.primary,
        cancelButtonColor: Color = MaterialTheme.colorScheme.primary,
        cancelDownloadButtonColor: Color = ButtonDefaults.buttonColors().containerColor,
    ): ColorScheme = ColorScheme(
        downloadButtonColor = downloadButtonColor,
        cancelButtonColor = cancelButtonColor,
        cancelDownloadButtonColor = cancelDownloadButtonColor,
    )
}



