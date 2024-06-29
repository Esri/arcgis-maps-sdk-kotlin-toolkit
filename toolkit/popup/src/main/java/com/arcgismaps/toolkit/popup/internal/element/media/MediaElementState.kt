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

import android.content.Context
import android.graphics.drawable.BitmapDrawable
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
import coil.imageLoader
import coil.request.ImageRequest
import com.arcgismaps.mapping.ChartImageParameters
import com.arcgismaps.mapping.ChartImageStyle
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupMedia
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import com.arcgismaps.toolkit.popup.internal.util.MediaImageProvider
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
    override val id : Int = createId()
) : PopupElementState() {

    constructor(mediaPopupElement: MediaPopupElement, scope: CoroutineScope, mediaFolder: String, chartParams: ChartImageParameters, context: Context): this(
        description = mediaPopupElement.description,
        title = mediaPopupElement.title,
        media = mediaPopupElement.media.map {
            if (it.type.isChart) {
                PopupMediaState(
                    it,
                    scope,
                    MediaImageProvider(
                        fileName = "media-${Objects.hash(mediaPopupElement.title, it.title, it.caption)}",
                        folderName = mediaFolder
                    ) {
                        it.generateChart(chartParams).getOrThrow().image.bitmap
                    }
                )
            } else if (it.type is PopupMediaType.Image) {
                val srcUrl = it.value?.sourceUrl
                    ?: throw IllegalArgumentException("null sourceUrl for popup media")
                PopupMediaState(
                    it,
                    scope,
                    MediaImageProvider(
                        fileName = "media-${Objects.hash(srcUrl)}",
                        folderName = mediaFolder
                    ) {
                        val request = ImageRequest.Builder(context)
                            .data(srcUrl)
                            .crossfade(true)
                            .build()
                        (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                            ?: throw IllegalStateException("couldn't load image at $srcUrl")
                    }
                )

            } else {
                throw IllegalArgumentException("unknown media type")
            }

        }
    )

    companion object {
        fun Saver(
            element: MediaPopupElement,
            scope: CoroutineScope,
            chartFolder: String,
            chartParams: ChartImageParameters,
            context: Context
        ): Saver<MediaElementState, Any> = Saver(
            save = { null },
            restore = {
                MediaElementState(
                    element, scope, chartFolder, chartParams, context
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
    val context = LocalContext.current
    return rememberSaveable(
        inputs = arrayOf(popup, element),
        saver = MediaElementState.Saver(element, scope, mediaFolder, chartParams, context)
    ) {
        MediaElementState(
            element,
            scope,
            mediaFolder,
            chartParams,
            context
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
 * @property imageGenerator a lambda which generates charts. Is only invoked if type is chart.
 */
internal class PopupMediaState(
    val title: String,
    val caption: String,
    @Suppress("unused") val refreshInterval: Long,
    @Suppress("unused") val linkUrl: String,
    private val sourceUrl: String,
    val type: PopupMediaType,
    private val scope: CoroutineScope,
    private val imageGenerator: MediaImageProvider
) {
    private val _imageUri: MutableState<String> = mutableStateOf("")

    /**
     * Provides the URI to display the image in an AsyncImage composable call.
     */
    val imageUri: State<String> = _imageUri

    init {
        scope.launch {
            _imageUri.value = imageGenerator.get()
        }
    }

    constructor(
        media: PopupMedia,
        scope: CoroutineScope,
        imageGenerator: MediaImageProvider
    ) : this(
        title = media.title,
        caption = media.caption,
        refreshInterval = media.imageRefreshInterval,
        linkUrl = media.value?.linkUrl ?: "",
        sourceUrl = media.value?.sourceUrl ?: "",
        type = media.type,
        scope = scope,
        imageGenerator = imageGenerator
    )
}

private val PopupMediaType.isChart: Boolean
    get() = when(this) {
        is PopupMediaType.BarChart, is PopupMediaType.ColumnChart, is PopupMediaType.PieChart, is PopupMediaType.LineChart -> true
        else -> false
    }
