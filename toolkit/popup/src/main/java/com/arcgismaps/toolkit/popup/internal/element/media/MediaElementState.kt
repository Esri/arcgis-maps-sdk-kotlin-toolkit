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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.arcgismaps.mapping.ChartImageParameters
import com.arcgismaps.mapping.ChartImageStyle
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupMedia
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import com.arcgismaps.toolkit.popup.internal.util.ChartImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Objects

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

    constructor(mediaPopupElement: MediaPopupElement, scope: CoroutineScope, chartFolder: String, chartParams: ChartImageParameters): this(
        description = mediaPopupElement.description,
        title = mediaPopupElement.title,
        media = mediaPopupElement.media.map { PopupMediaState(it, scope, chartFolder, chartParams) }
    )

    companion object {
        fun Saver(
            element: MediaPopupElement,
            scope: CoroutineScope,
            chartFolder: String,
            chartParams: ChartImageParameters
        ): Saver<MediaElementState, Any> = Saver(
            save = { null },
            restore = {
                MediaElementState(
                    element, scope, chartFolder, chartParams
                )
            }
        )

    }
}

/**
 * Creates a state object for a PopupMediaElement.
 *
 * @param element a MediaPopupElement
 * @param popup the Popup which contains the element
 */
@Composable
internal fun rememberMediaElementState(
    element: MediaPopupElement,
    popup: Popup
): MediaElementState {
    val scope = rememberCoroutineScope()
    val darkMode = isSystemInDarkTheme()
    val defaults = MediaElementDefaults.shapes()
    val localDensity = LocalDensity.current
    val width = with(localDensity) {
        defaults.tileWidth.roundToPx()
    }
    val height = with(localDensity) {
        defaults.tileHeight.roundToPx()
    }

    // The chart image parameters are created here so they can use
    // composition locals to access screen density, dark theme, etc.
    val chartParams = remember(isSystemInDarkTheme()) {
        ChartImageParameters(width, height).apply {
            style = if (darkMode) {
                ChartImageStyle.Dark
            } else {
                ChartImageStyle.Light
            }

            this.screenScale = localDensity.density
        }
    }
    // the composition local context provides the cacheDir to be ultimately passed into
    // the chart provider so charts can be saved to disk.
    val mediaFolder = "${LocalContext.current.cacheDir.canonicalPath}/popup_media"
    return rememberSaveable(
        inputs = arrayOf(popup, element),
        saver = MediaElementState.Saver(element, scope, mediaFolder, chartParams)
    ) {
        MediaElementState(
            element,
            scope,
            mediaFolder,
            chartParams
        )
    }
}

/**
 * Represents the state of a [PopupMedia value].
 *
 * @property title the title from the PopupMedia
 * @property caption the caption from the PopupMedia
 * @property refreshInterval the PopupMedia refresh interval
 * @property linkUrl the link to use to view the media for image type media in the media viewer
 * @property sourceUrl the link to use to render the image for image type media
 * @property type the type of the PopupMedia
 * @property scope a CoroutineScope to use to acquire chart images
 * @property chartFolder the folder in which to save chart images
 * @property imageGenerator a lambda which generates charts. Is only invoked if type is chart.
 */
internal class PopupMediaState(
    val title: String,
    val caption: String,
    @Suppress("unused") val refreshInterval: Long,
    @Suppress("unused") val linkUrl: String,
    val sourceUrl: String,
    val type: PopupMediaType,
    private val scope: CoroutineScope,
    private val chartFolder: String,
    private val imageGenerator: (suspend () -> Bitmap)? = null
) {
    private val _imageUri: MutableState<String> = mutableStateOf("")

    /**
     * Provides the URI to display the image in an AsyncImage composable call.
     */
    val imageUri: State<String> = _imageUri

    init {
        // charts only
        if (!(type is PopupMediaType.Image || type is PopupMediaType.Unknown)) {
            scope.launch {
                val fileName = "media-${Objects.hash(sourceUrl)}.png"
                imageGenerator?.let {
                    val provider = ChartImageProvider(fileName, chartFolder, it)
                    _imageUri.value = provider.get()
                }

            }
        } else {
            _imageUri.value = sourceUrl
        }
    }

    constructor(
        media: PopupMedia,
        scope: CoroutineScope,
        chartFolder: String,
        chartParams: ChartImageParameters
    ) : this(
        title = media.title,
        caption = media.caption,
        refreshInterval = media.imageRefreshInterval,
        linkUrl = media.value?.linkUrl ?: "",
        sourceUrl = media.value?.sourceUrl ?: "",
        type = media.type,
        scope = scope,
        chartFolder = chartFolder,
        imageGenerator = {
            media.generateChart(chartParams).getOrThrow().image.bitmap
        }
    )
}
