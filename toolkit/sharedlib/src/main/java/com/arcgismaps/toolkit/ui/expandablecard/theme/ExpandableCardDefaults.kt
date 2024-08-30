/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.ui.expandablecard.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard

/**
 * Contains the default values used by [ExpandableCard].
 */
object ExpandableCardDefaults {

    /**
     * Creates a [ExpandableCardColorScheme] with default values.
     *
     * @param headerTextColor The color scheme for the card header
     * @param containerColor The color scheme for the card container
     * @param galleryContainerColor The color scheme for the card's gallery, if any.
     * @param borderColor The color scheme for the card's border.
     * @since 200.6.0
     */
    @Composable
    fun colorScheme(
        headerTextColor: Color = MaterialTheme.colorScheme.onBackground,
        readOnlyTextColor: Color = Color.Unspecified,
        headerButtonTextColor: Color = MaterialTheme.colorScheme.onPrimary,
        containerColor: Color = MaterialTheme.colorScheme.background,
        galleryContainerColor: Color = MaterialTheme.colorScheme.onBackground,
        borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    ): ExpandableCardColorScheme {
        return ExpandableCardColorScheme(
            headerTextColor = headerTextColor,
            readOnlyTextColor = readOnlyTextColor,
            headerButtonTextColor = headerButtonTextColor,
            containerColor = containerColor,
            galleryContainerColor = galleryContainerColor,
            borderColor = borderColor
        )
    }

    /**
     * Creates a [ExpandableCardTypography] with default values.
     *
     * @param headerStyle The typography for the card header
     * @param bodyStyle The typography for card body
     * @since 200.6.0
     */
    @Composable
    fun typography(
        headerStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        bodyStyle: TextStyle = MaterialTheme.typography.bodyMedium,
        readOnlyTextStyle: TextStyle = MaterialTheme.typography.bodyLarge
    ): ExpandableCardTypography {
        return ExpandableCardTypography(
            headerStyle = headerStyle,
            bodyStyle = bodyStyle,
            readOnlyTextStyle = readOnlyTextStyle
        )
    }

    /**
     * Creates a [ExpandableCardShapes] with default values.
     *
     * @param padding The internal padding of the card body
     * @param containerShape the shape of the card
     * @param borderThickness the thickness of the card border
     * @since 200.6.0
     */
    @Composable
    fun shapes(
        padding: Dp = 16.dp,
        containerShape: RoundedCornerShape = RoundedCornerShape(5.dp),
        borderThickness: Dp = 1.dp
    ): ExpandableCardShapes {
        return ExpandableCardShapes(
            padding = padding,
            containerShape = containerShape,
            borderThickness = borderThickness
        )
    }
}
