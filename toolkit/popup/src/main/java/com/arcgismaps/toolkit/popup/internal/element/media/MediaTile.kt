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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.toolkit.popup.internal.util.PopupMediaImageLoader
import com.arcgismaps.toolkit.popup.internal.util.RemoteImageLoader

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

        val placeholder = state.type.rememberPlaceholder()
        val getter by remember(state.sourceUrl) {
            mutableStateOf(
                RemoteImageLoader(
                    placeholder,
                    state.sourceUrl
                )
            )
        }
        MediaView(
            mediaImageLoader = getter,
            title = state.title,
            caption = state.caption
        )
    }
}

/**
 * Loads an image asynchronously using the [PopupMediaImageLoader].
 */
@Composable
private fun MediaImage(
    mediaImageLoader: PopupMediaImageLoader,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.FillBounds,
    contentDescription: String = " ",
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    LaunchedEffect(mediaImageLoader) {
        mediaImageLoader.get(context)
    }
    val painter = mediaImageLoader.image.value
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
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
    title: String,
    caption: String,
    mediaImageLoader: PopupMediaImageLoader,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        MediaImage(
            mediaImageLoader = mediaImageLoader,
            contentDescription = title,
            modifier = Modifier.fillMaxSize()
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

@Preview(showBackground = true)
@Composable
internal fun PreviewMediaTile() {
    MediaTile(
        state = PopupMediaState(
            title = "Some attachment",
            caption = "foo",
            refreshInterval = 1234L,
            sourceUrl = "https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png",
            linkUrl = "",
            type = PopupMediaType.Image
        )
    )
}

@Composable
private fun PopupMediaType.rememberPlaceholder(): VectorPainter = when (this) {
    PopupMediaType.Image -> rememberVectorPainter(image = Icons.Outlined.Image)
    else -> rememberVectorPainter(image = Icons.Outlined.BarChart)
}

