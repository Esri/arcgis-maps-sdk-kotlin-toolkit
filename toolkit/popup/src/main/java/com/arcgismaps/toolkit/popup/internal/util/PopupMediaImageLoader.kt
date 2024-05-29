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

package com.arcgismaps.toolkit.popup.internal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A model to asynchronously acquire a Popup Media image. This class provides an abstraction layer
 * above Chart and remote URL images. Once the target image is acquired it is presented via [image]
 * State.
 *
 * @param placeholder the placeholder image to show until the intended image is acquired.
 * @param getter a suspending lambda to acquire the image
 */
internal sealed class PopupMediaImageLoader(
    placeholder: Painter,
    private val getter: suspend (Context) -> Bitmap
) {
    private val _image: MutableState<Painter> = mutableStateOf(placeholder)
    val image: State<Painter> = _image

    suspend fun get(context: Context) {
        _image.value = BitmapPainter(getter(context).asImageBitmap())
    }
}

internal class RemoteImageLoader(
    placeholder: Painter,
    private val url: String
): PopupMediaImageLoader(placeholder, getter = { context ->
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
        (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
            ?: throw IllegalStateException("couldn't load image at $url")
    }
})

//
//internal class ChartImageLoader(
//    media: PopupMedia,
//    chartParameters: ChartImageParameters,
//    placeholder: Painter
//): ImageGetter(placeholder, getter = { _ ->
//    media.generateChartAsync(chartParameters).getOrThrow().image
//})