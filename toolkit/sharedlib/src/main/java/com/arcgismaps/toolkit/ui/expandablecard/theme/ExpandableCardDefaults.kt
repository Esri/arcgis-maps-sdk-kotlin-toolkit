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
     * @param borderColor The color scheme for the card's border.
     * @since 200.6.0
     */
    @Composable
    fun colorScheme(
        headerTextColor: Color = MaterialTheme.colorScheme.onBackground,
        containerColor: Color = MaterialTheme.colorScheme.background,
        borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    ): ExpandableCardColorScheme {
        return ExpandableCardColorScheme(
            headerTextColor = headerTextColor,
            containerColor = containerColor,
            borderColor = borderColor
        )
    }

    /**
     * Creates a [ExpandableCardShapes] with default values.
     *
     * @param containerShape the shape of the card
     * @since 200.6.0
     */
    @Composable
    fun shapes(
        containerShape: RoundedCornerShape = RoundedCornerShape(5.dp),
    ): ExpandableCardShapes {
        return ExpandableCardShapes(
            containerShape = containerShape
        )
    }
}
