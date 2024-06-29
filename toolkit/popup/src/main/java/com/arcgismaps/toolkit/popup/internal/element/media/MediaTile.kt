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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arcgismaps.mapping.popup.PopupMediaType

@Composable
internal fun MediaTile(
    state: PopupMediaState
) {
    val colors = MediaElementDefaults.colors()
    val shapes = MediaElementDefaults.shapes()
    val model by state.imageUri
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
        val defaults = MediaElementDefaults.shapes()
        val padding = if (state.type is PopupMediaType.Image)
            defaults.mediaImagePadding
        else defaults.mediaChartPadding
        MediaView(
            model = model,
            title = state.title,
            caption = state.caption,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun Title(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    Text(
        text = text,
        color = color,
        style = style,
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = modifier.padding(vertical = 1.dp)
    )
}

@Composable
private fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Text(
        text = text,
        color = color,
        style = style,
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
internal fun MediaView(
    model: String,
    title: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AsyncImage(
            model = model,
            contentDescription = title,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.fillMaxSize(),
            alignment = Alignment.Center,
            alpha = DefaultAlpha,
            colorFilter = null
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(
                    if (title.isNotEmpty() && caption.isNotEmpty()) {
                        40.dp
                    } else if (title.isNotEmpty() || caption.isNotEmpty()) {
                        20.dp
                    } else {
                        0.dp
                    }
                )
                .background(
                    MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.7f
                    )
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Title(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                color = MaterialTheme.colorScheme.background
            )
            Caption(
                text = caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
            )

        }
    }
}
