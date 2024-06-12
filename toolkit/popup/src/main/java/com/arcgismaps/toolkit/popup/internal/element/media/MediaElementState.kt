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

package com.arcgismaps.toolkit.popup.internal.element.media

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import com.arcgismaps.mapping.ChartImageParameters
import com.arcgismaps.mapping.ChartImageStyle
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupMedia
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import com.arcgismaps.toolkit.popup.internal.util.rememberChartImagePainter
import com.arcgismaps.toolkit.popup.internal.util.rememberMediaImagePainter

/**
 * Represents the state of an [MediaPopupElement]
 */
@Immutable
internal class MediaElementState(
    val description: String,
    val title: String,
    val media: List<PopupMediaState>,
    override val id : Int = createId(),
) : PopupElementState() {

    constructor(mediaPopupElement: MediaPopupElement): this(
        description = mediaPopupElement.description,
        title = mediaPopupElement.title,
        media = mediaPopupElement.media.map { PopupMediaState(it) }
    )

    companion object {
        fun Saver(
            element: MediaPopupElement
        ): Saver<MediaElementState, Any> = Saver(
            save = { null },
            restore = {
                MediaElementState(
                    element
                )
            }
        )

    }
}

@Composable
internal fun rememberMediaElementState(
    element: MediaPopupElement,
    popup: Popup
): MediaElementState {
    return rememberSaveable(
        inputs = arrayOf(popup, element),
        saver = MediaElementState.Saver(element)
    ) {
        MediaElementState(
            element
        )
    }
}

/**
 * Represents the state of a [PopupMedia value].
 */
internal class PopupMediaState(
    val title: String,
    val caption: String,
    @Suppress("unused") val refreshInterval: Long,
    @Suppress("unused") val linkUrl: String,
    val sourceUrl: String,
    val type: PopupMediaType,
    val imageGenerator: (suspend (ChartImageParameters) -> Bitmap)? = null
) {
    constructor(media: PopupMedia) : this(
        title = media.title,
        caption = media.caption,
        refreshInterval = media.imageRefreshInterval,
        linkUrl = media.value?.linkUrl ?: "",
        sourceUrl = media.value?.sourceUrl ?: "",
        type = media.type,
        imageGenerator = { params -> media.generateChart(params).getOrThrow().image.bitmap }
    )
}

@Composable
internal fun PopupMediaState.rememberMediaPainter(): State<Painter> =
    when (type) {
        is PopupMediaType.Image -> rememberMediaImagePainter(sourceUrl)
        else -> {
            val darkMode = isSystemInDarkTheme()
            val defaults = MediaElementDefaults.shapes()
            val localDensity = LocalDensity.current
            val width = with(localDensity) {
                defaults.tileWidth.roundToPx()
            }
            val height = with(localDensity) {
                defaults.tileHeight.roundToPx()
            }

            val params = remember(this, isSystemInDarkTheme()) {
                ChartImageParameters(width, height).apply {
                    style = if (darkMode) {
                        ChartImageStyle.Dark
                    } else {
                        ChartImageStyle.Light
                    }

                    this.screenScale = localDensity.density
                }
            }
            rememberChartImagePainter(
                key = this,
                chartParameters = params,
                chartGenerator = imageGenerator!!
            )
        }
    }
