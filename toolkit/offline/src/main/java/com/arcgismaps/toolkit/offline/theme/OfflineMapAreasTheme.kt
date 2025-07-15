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

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
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
 * @param offlineOpenButtonText The text style for the open button in Offline Map Areas.
 * @param preplannedMapAreaTitle The text style for the title of preplanned map area.
 * @param preplannedMapAreaDescription The text style for the description of preplanned map area.
 * @param preplannedMapAreaStatus The text style for the status of preplanned map area.
 * @param mapAreasDetailsTitle The text style for the title in map areas details.
 * @param mapAreasDetailsSize The text style for the size label in map areas details.
 * @param mapAreasDetailsDescriptionLabel The text style for the description label in map areas details.
 * @param mapAreasDetailsDescription The text style for the description in map areas details.
 * @param onDemandMapAreasTitle The text style for the title of on-demand map areas.
 * @param onDemandMapAreaStatus The text style for the status of on-demand map area.
 * @param onDemandMapAreaAddMapAreaButtonText The text style for the add map area button in on-demand map area.
 * @param onDemandMapAreaSelectorTitle The text style for the title in on-demand map area selector.
 * @param onDemandMapAreaSelectorMessage The text style for the message in on-demand map area selector.
 * @param onDemandMapAreaSelectorAreaName The text style for the area name in on-demand map area selector.
 * @param onDemandMapAreaSelectorRenameButtonTextStyle The text style for the rename button in on-demand map area selector.
 * @param areaNameDialogTitle The text style for the title of the area name dialog in on-demand map area selector.
 *
 * @since 200.8.0
 */
@Immutable
@ExposedCopyVisibility
public data class Typography internal constructor(
    val offlineMapAreasTitle: TextStyle,
    val offlineOpenButtonText: TextStyle,

    val preplannedMapAreaTitle: TextStyle,
    val preplannedMapAreaDescription: TextStyle,
    val preplannedMapAreaStatus: TextStyle,

    val mapAreasDetailsTitle: TextStyle,
    val mapAreasDetailsSize: TextStyle,
    val mapAreasDetailsDescriptionLabel: TextStyle,
    val mapAreasDetailsDescription: TextStyle,

    val onDemandMapAreasTitle: TextStyle,
    val onDemandMapAreaStatus: TextStyle,
    val onDemandMapAreaAddMapAreaButtonText: TextStyle,

    val onDemandMapAreaSelectorTitle: TextStyle,
    val onDemandMapAreaSelectorMessage: TextStyle,
    val onDemandMapAreaSelectorAreaName: TextStyle,
    val onDemandMapAreaSelectorRenameButtonTextStyle: TextStyle,

    val areaNameDialogTitle: TextStyle
)

/**
 * Color styling properties for the OfflineMapAreas composable.
 *
 * This class defines the color scheme used for various buttons and background in the OfflineMapAreas UI,
 * ensuring a consistent and visually appealing design.
 *
 * @param offlineBackgroundColor The background color used for the offline map areas.
 * @param offlineSurfaceContainerColor The color used for the background of offline map area descriptions.
 * @param offlineIconButtonsColor The color used for the icon buttons in offline map areas.
 * @param offlineButtonsColor The color used for the buttons in offline map areas.
 * @param onDemandMapAreaSelectorCancelButtonColor The color used for the cancel button in the on-demand map area selector.
 *
 * @since 200.8.0
 */
@Immutable
@ExposedCopyVisibility
public data class ColorScheme internal constructor(
    val offlineBackgroundColor: Color,
    val offlineSurfaceContainerColor: Color,
    val offlineIconButtonsColor: Color,
    val offlineButtonsColor: ButtonColors,
    val onDemandMapAreaSelectorCancelButtonColor: IconButtonColors
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
     * @param offlineOpenButtonText The text style for the open button in offline map areas.
     * @param preplannedMapAreaTitle The text style for the title of preplanned map area.
     * @param preplannedMapAreaDescription The text style for the description of preplanned map area.
     * @param preplannedMapAreaStatus The text style for the status of preplanned map area.
     * @param mapAreasDetailsTitle The text style for the title in map areas details.
     * @param mapAreasDetailsSize The text style for the size label in map areas details.
     * @param mapAreasDetailsDescriptionLabel The text style for the description label in map areas details.
     * @param mapAreasDetailsDescription The text style for the description in map areas details.
     * @param onDemandMapAreasTitle The text style for the title of on-demand map areas.
     * @param onDemandMapAreaStatus The text style for the status of on-demand map area.
     * @param onDemandMapAreaAddMapAreaButtonText The text style for the add map area button in on-demand map area.
     * @param onDemandMapAreaSelectorTitle The text style for the title in on-demand map area selector.
     * @param onDemandMapAreaSelectorMessage The text style for the message in on-demand map area selector.
     * @param onDemandMapAreaSelectorAreaName The text style for the area name in on-demand map area selector.
     * @param onDemandMapAreaSelectorRenameButtonTextStyle The text style for the rename button in on-demand map area selector.
     * @param areaNameDialogTitle The text style for the title of the area name dialog in on-demand map area selector.
     *
     * @since 200.8.0
     */
    @Composable
    public fun typography(
        offlineMapAreasTitle: TextStyle = MaterialTheme.typography.titleMedium,
        offlineOpenButtonText: TextStyle = MaterialTheme.typography.labelSmall,
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
        onDemandMapAreaAddMapAreaButtonText:  TextStyle = MaterialTheme.typography.labelSmall,
        onDemandMapAreaSelectorTitle: TextStyle = MaterialTheme.typography.titleMedium,
        onDemandMapAreaSelectorMessage: TextStyle = MaterialTheme.typography.labelSmall,
        onDemandMapAreaSelectorAreaName: TextStyle = MaterialTheme.typography.titleLarge,
        onDemandMapAreaSelectorRenameButtonTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
        areaNameDialogTitle: TextStyle = MaterialTheme.typography.titleLarge
    ): Typography = Typography(
        offlineMapAreasTitle = offlineMapAreasTitle,
        offlineOpenButtonText = offlineOpenButtonText,
        preplannedMapAreaTitle = preplannedMapAreaTitle,
        preplannedMapAreaDescription = preplannedMapAreaDescription,
        preplannedMapAreaStatus = preplannedMapAreaStatus,
        mapAreasDetailsTitle = mapAreasDetailsTitle,
        mapAreasDetailsSize = mapAreasDetailsSize,
        mapAreasDetailsDescriptionLabel = mapAreasDetailsDescriptionLabel,
        mapAreasDetailsDescription = mapAreasDetailsDescription,
        onDemandMapAreasTitle = onDemandMapAreasTitle,
        onDemandMapAreaStatus = onDemandMapAreaStatus,
        onDemandMapAreaAddMapAreaButtonText = onDemandMapAreaAddMapAreaButtonText,
        onDemandMapAreaSelectorTitle = onDemandMapAreaSelectorTitle,
        onDemandMapAreaSelectorMessage = onDemandMapAreaSelectorMessage,
        onDemandMapAreaSelectorAreaName = onDemandMapAreaSelectorAreaName,
        onDemandMapAreaSelectorRenameButtonTextStyle = onDemandMapAreaSelectorRenameButtonTextStyle,
        areaNameDialogTitle = areaNameDialogTitle
    )

    /**
     * Creates default color scheme values for the OfflineMapAreas composable.
     *
     * @param offlineBackgroundColor The background color used for the offline map areas.
     * @param offlineSurfaceContainerColor The color used for the background of offline map area descriptions.
     * @param offlineIconButtonsColor The color used for the icon buttons in offline map areas.
     * @param offlineButtonsColor The color used for the buttons in offline map areas.
     * @param onDemandMapAreaSelectorCancelButtonColor The color used for the cancel button in the on-demand map area selector.
     *
     * @since 200.8.0
     */
    @Composable
    public fun colorScheme(
        offlineBackgroundColor: Color = MaterialTheme.colorScheme.background,
        offlineSurfaceContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        offlineIconButtonsColor: Color = MaterialTheme.colorScheme.primary,
        offlineButtonsColor: ButtonColors = ButtonDefaults.buttonColors(),
        onDemandMapAreaSelectorCancelButtonColor: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
    ): ColorScheme = ColorScheme(
        offlineBackgroundColor = offlineBackgroundColor,
        offlineSurfaceContainerColor = offlineSurfaceContainerColor,
        offlineIconButtonsColor = offlineIconButtonsColor,
        offlineButtonsColor = offlineButtonsColor,
        onDemandMapAreaSelectorCancelButtonColor = onDemandMapAreaSelectorCancelButtonColor
    )
}



