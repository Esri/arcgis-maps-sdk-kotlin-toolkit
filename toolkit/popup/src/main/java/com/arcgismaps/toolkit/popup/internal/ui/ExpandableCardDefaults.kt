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

package com.arcgismaps.toolkit.popup.internal.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


internal object ExpandableCardDefaults {
    @Composable fun shapes(): ExpandableCardShapes = ExpandableCardShapes(
        padding = 16.dp,
        containerShape = RoundedCornerShape(5.dp),
        borderThickness = 1.dp
    )
    @Composable
    fun colors() : AttachmentElementColors = AttachmentElementColors(
        containerColor = MaterialTheme.colorScheme.background,
        galleryContainerColor = MaterialTheme.colorScheme.onBackground,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

internal data class ExpandableCardShapes(
    val padding: Dp,
    val containerShape: RoundedCornerShape,
    val borderThickness: Dp
)

internal data class AttachmentElementColors(
    val containerColor : Color,
    val galleryContainerColor: Color,
    val borderColor : Color,
)

