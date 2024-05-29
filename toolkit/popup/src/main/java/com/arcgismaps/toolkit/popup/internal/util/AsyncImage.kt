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
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads an image asynchronously using the [ImageGetter].
 */
@Composable
internal fun MediaImage(
    imageGetter: ImageGetter,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    LaunchedEffect(imageGetter) {
        imageGetter.get(context)
    }
    val painter = imageGetter.image.value
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

/**
 * A model to asynchronously load the image. Once the loading is complete
 * the loaded image is presented via [image] State.
 *
 * @param placeholder the placeholder image to show until the loading is complete.
 * @param getter a suspending lambda to get the image
 */
internal open class ImageGetter(
    placeholder: Painter,
    private val getter: suspend (Context) -> Bitmap
) {
    private val _image: MutableState<Painter> = mutableStateOf(placeholder)
    val image: State<Painter> = _image

    suspend fun get(context: Context) {
        _image.value = BitmapPainter(getter(context).asImageBitmap())
    }
}

internal class RemoteImageGetter(
    placeholder: Painter,
    private val url: String
): ImageGetter(placeholder, getter = { context ->
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
//internal class ChartImageGetter(
//    media: PopupMedia,
//    chartParameters: ChartImageParameters,
//    placeholder: Painter,
//    private val url: String
//): ImageGetter(placeholder, getter = { _ ->
//    media.generateChartAsync(chartParameters).getOrThrow().image
//})