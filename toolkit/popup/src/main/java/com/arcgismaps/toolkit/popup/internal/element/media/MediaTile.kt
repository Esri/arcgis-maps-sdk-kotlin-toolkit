/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.popup.internal.element.media

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.mapping.popup.PopupMediaValue
import com.arcgismaps.toolkit.popup.internal.util.MediaImage
import com.arcgismaps.toolkit.popup.internal.util.RemoteImageGetter

@Composable
internal fun MediaTile(
    state: PopupMediaState
) {
    val colors = MediaElementDefaults.colors()
    val shapes = MediaElementDefaults.shapes()
    Box(
        modifier = Modifier
            .width(shapes.tileWidth)
            .height(shapes.tileHeight)
            .clip(shape = shapes.tileShape)
            .border(
                border = BorderStroke(shapes.tileStrokeWidth, colors.tileBorderColor),
                shape = shapes.tileShape
            )
            .clickable {
                // TODO open media viewer here
            }
    ) {

        val placeholder = rememberVectorPainter(image = Icons.Rounded.Image)
        val getter by remember(state.media) { mutableStateOf(RemoteImageGetter(placeholder, state.media.sourceUrl!!)) }
        MediaImage(imageGetter = getter, contentDescription = "foo")
    }
}

@Preview(showBackground = true)
@Composable
internal fun PreviewMediaTile() {
    MediaTile(
        state = PopupMediaState(
            title = "Some attachment",
            caption = "foo",
            refreshInterval = 1234L,
            media = PopupMediaValue().apply { sourceUrl = "https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png"},
            type = PopupMediaType.Image
        )
    )
}
