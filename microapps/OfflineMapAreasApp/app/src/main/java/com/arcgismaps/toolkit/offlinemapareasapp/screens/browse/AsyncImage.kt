/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.offlinemapareasapp.screens.browse

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
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
import com.arcgismaps.portal.LoadableImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.launch

/**
 * Loads an image asynchronously using the [ImageLoader].
 */
@Composable
fun AsyncImage(
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val painter = imageLoader.image.value
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

/**
 * A model to asynchronously load the image from a [LoadableImage]. Once the loading is complete
 * the loaded image is presented via [image] State.
 *
 * @param loadable the [LoadableImage] to load.
 * @param scope the CoroutineScope to run the loading job on.
 * @param placeholder the placeholder image to show until the loading is complete.
 */
class ImageLoader(
    private val loadable: LoadableImage,
    scope: CoroutineScope,
    placeholder: Painter,
) {
    private val _image: MutableState<Painter> = mutableStateOf(placeholder)
    val image: State<Painter> = _image

    init {
        scope.launch(start = UNDISPATCHED) {
            load()
        }
    }

    private suspend fun load() {
        loadable.load().onSuccess {
            loadable.image?.let {
                _image.value = BitmapPainter(it.bitmap.asImageBitmap())
            }
        }
    }
}
