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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.toolkit.orientedimageryviewer.internal.media.PanoramicImage
import com.arcgismaps.toolkit.orientedimageryviewer.internal.media.RasterImage

@Composable
internal fun ImageryView(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 520.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        val activeImage = viewerState.activeImageInfo
        if (activeImage == null) {
            // TODO: Show a placeholder or error message for when there is no active image.
            return@Box
        } else if (activeImage.dataUri == null) {
            // TODO: Show a placeholder or error message for when the active image has no data URI.
            return@Box
        }
        val imageUri = activeImage.dataUri
        when (activeImage.type) {
                OrientedImageType.Horizontal,
                OrientedImageType.Inspection,
                OrientedImageType.Nadir,
                OrientedImageType.Oblique -> {
                    RasterImage(viewerState, modifier = Modifier.fillMaxSize())
                }
                OrientedImageType.Image360 -> {
                    PanoramicImage(imageSource = imageUri)
                }
                else -> {
                    // TODO: Show a placeholder or error message for unsupported image types including video.
                }
        }
    }
}
