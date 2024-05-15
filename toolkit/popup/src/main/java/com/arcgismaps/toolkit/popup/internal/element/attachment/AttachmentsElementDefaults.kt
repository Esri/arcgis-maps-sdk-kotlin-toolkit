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

package com.arcgismaps.toolkit.popup.internal.element.attachment




import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A central place for theming values. To be promoted to a public theme type.
 */
internal object AttachmentsElementDefaults {
    @Composable
    fun shapes(): AttachmentElementShapes = AttachmentElementShapes(
        borderThickness = 1.dp,
        containerShape = RoundedCornerShape(5.dp),
        tileShape = RoundedCornerShape(8.dp),
        galleryPadding = 15.dp,
        tileStrokeWidth = 0.5.dp,
        tileWidth = 92.dp,
        tileHeight = 75.dp
    )

    @Suppress("unused")
    @Composable
    fun colors() : AttachmentsElementColors = AttachmentsElementColors(
        containerColor = MaterialTheme.colorScheme.background,
        galleryContainerColor = MaterialTheme.colorScheme.onBackground,
        borderColor = MaterialTheme.colorScheme.outline,
        tileBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    )
}

internal data class AttachmentElementShapes(
    val borderThickness: Dp,
    val containerShape: RoundedCornerShape,
    val tileShape: RoundedCornerShape,
    val galleryPadding: Dp,
    val tileStrokeWidth: Dp,
    val tileWidth: Dp,
    val tileHeight: Dp
)

internal data class AttachmentsElementColors(
    val containerColor : Color,
    val galleryContainerColor: Color,
    val tileBorderColor : Color,
    val borderColor : Color
)

