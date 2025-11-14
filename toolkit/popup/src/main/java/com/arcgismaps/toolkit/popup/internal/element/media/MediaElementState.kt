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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.arcgismaps.mapping.ChartImageParameters
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupMedia
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.popup.R
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import com.arcgismaps.toolkit.popup.internal.util.MediaImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal object PopupConstants {
    const val USER_AGENT = "ArcGISMaps-Kotlin"
}

/**
 * Represents the state of an [MediaPopupElement]
 */
@Immutable
internal class MediaElementState(
    val element: MediaPopupElement,
    val popup: Popup,
    val scope: CoroutineScope,
    override val id : Int = createId()
) : PopupElementState() {

    val description: String = element.description
    val title: String = element.title
    var media: List<PopupMediaState> = emptyList()

    /**
     * Indicates if the media list for the [popup] has been created.
     */
    internal var isPopupMediaCreated: Boolean by mutableStateOf(false)
        private set

    fun createPopupMedia(
        mediaFolder: String,
        chartParams: ChartImageParameters,
        context: Context,
        models: List<String> = listOf()
    ) {
        this.media = element.media.mapIndexed { index, media ->
            val model = models.getOrNull(index) ?: ""
            if (media.type.isChart) {
                PopupMediaState.createChartMediaState(media, model, scope, mediaFolder, chartParams)
            } else {
                PopupMediaState.createImageMediaState(media, model, scope, mediaFolder, context)
            }
        }

        val geoElement = this.popup.geoElement
        if (geoElement is DynamicEntity) {
            // For dynamic entities
            // update chart providers to use the new instances of PopupMedia to reacquire updated charts.
            updateMediaElement(element, scope)
        }
        isPopupMediaCreated = true
    }


    /**
     * Update the PopupMedia so that a new chart image can be acquired. Only necessary
     * for DynamicEntity Popups.
     *
     * @param newElement the new MediaPopupElement which contains the new PopupMedia
     * @param scope the current CoroutineScope of the Composition.
     */
    internal fun updateMediaElement(newElement: MediaPopupElement, scope: CoroutineScope) {
        newElement.media.forEachIndexed { index, medium ->
            if (medium.type.isChart) {
                media.getOrNull(index)?.updateMedia(medium, scope)
            }
        }
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
 * @param uri the path to the media image omn disk, or empty if the image is not yet persisted.
 * @param scope a CoroutineScope to use to acquire chart images
 * @property imageGenerator a lambda which generates charts. Is only invoked if type is chart.
 */
internal class PopupMediaState(
    val title: String,
    val caption: String,
    private val refreshInterval: Long,
    @Suppress("unused") private val linkUrl: String,
    @Suppress("unused") private val sourceUrl: String,
    val type: PopupMediaType,
    uri: String,
    scope: CoroutineScope,
    private val imageGenerator: MediaImageProvider
) {
    private val _imageUri: MutableState<String> = mutableStateOf("")

    /**
     * Provides the URI to display the image in an AsyncImage composable call.
     */
    val imageUri: State<String> = _imageUri

    init {
        if (uri.isNotEmpty()) {
            _imageUri.value = uri
        } else {
            scope.launch {
                _imageUri.value = imageGenerator.get("${UUID.randomUUID()}.png")
            }
        }
    }

    constructor(
        media: PopupMedia,
        scope: CoroutineScope,
        uri: String,
        imageGenerator: MediaImageProvider
    ) : this(
        title = media.title,
        caption = media.caption,
        refreshInterval = media.imageRefreshInterval,
        linkUrl = media.value?.linkUrl ?: "",
        sourceUrl = media.value?.sourceUrl ?: "",
        type = media.type,
        uri = uri,
        scope = scope,
        imageGenerator = imageGenerator
    )

    internal fun updateMedia(media: PopupMedia, scope: CoroutineScope) {
        imageGenerator.media = media
        scope.launch {
            val oldMedia = File(_imageUri.value)
            val img = imageGenerator.get("${UUID.randomUUID()}.png")
            _imageUri.value = img
            if (oldMedia.exists()) {
                oldMedia.delete()
            }
        }
    }

    companion object {
        internal fun createChartMediaState(
            media: PopupMedia,
            model: String,
            scope: CoroutineScope,
            imageFolder: String,
            chartParams: ChartImageParameters
        ): PopupMediaState = PopupMediaState(
            media,
            scope,
            model,
            MediaImageProvider(
                folderName = imageFolder,
                media = media,
            ) {
                it.generateChart(chartParams).getOrThrow().image.bitmap
            }
        )

        internal fun createImageMediaState(
            media: PopupMedia,
            model: String,
            scope: CoroutineScope,
            imageFolder: String,
            context: Context
        ): PopupMediaState {
            val srcUrl: String? = media.value?.sourceUrl ?: ""
            return PopupMediaState(
                media,
                scope,
                model,
                MediaImageProvider(
                    folderName = imageFolder,
                    media = media
                ) {
                    val request = ImageRequest.Builder(context)
                        .data(srcUrl)
                        .addHeader("User-Agent", PopupConstants.USER_AGENT)
                        .crossfade(true)
                        .build()
                    (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                        ?: ContextCompat.getDrawable(context, R.drawable.no_image_32)?.toBitmap()
                        ?: throw IllegalStateException(
                            if (srcUrl.isNullOrEmpty()) {
                                "couldn't load image: sourceUrl is missing or empty, and placeholder drawable could not be loaded"
                            } else {
                                "couldn't load image at $srcUrl, and placeholder drawable could not be loaded"
                            }
                        )
                }
            ).apply {
                if (refreshInterval > 0) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            // update the image according to the refresh interval
                            delay(refreshInterval)
                            updateMedia(media, scope)
                        }
                    }
                }
            }
        }
    }
}

private val PopupMediaType.isChart: Boolean
    get() = when(this) {
        is PopupMediaType.BarChart, is PopupMediaType.ColumnChart, is PopupMediaType.PieChart, is PopupMediaType.LineChart -> true
        else -> false
    }
