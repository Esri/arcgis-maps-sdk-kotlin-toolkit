/*
 * COPYRIGHT 1995-2024 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.utils

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.portal.LoadableImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Loads an image asynchronously using the [ImageLoader].
 */
@Composable
internal fun AsyncImage(
    imageLoader: ImageLoader<*>,
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
internal class ImageLoader<T: Loadable>(
    private val loadable: T,
    scope: CoroutineScope,
    placeholder: Painter,
    val acquireImage: suspend (T) -> Painter
) {
    private val _image: MutableState<Painter> = mutableStateOf(placeholder)
    val image: State<Painter> = _image

    private val loadStatus: StateFlow<LoadStatus> = loadable.loadStatus
    
    init {
        scope.launch {
            loadStatus.collect { status ->
                Log.d("ATTACH", "${(loadable as FormAttachment).name} has status $status")
                if (status is LoadStatus.Loaded) {
                    _image.value = acquireImage(loadable)
                }
            }
        }
    }
    
    @Suppress("unused")
    private suspend fun load() {
        loadable.load().onSuccess {
            _image.value = acquireImage(loadable)
        }
    }
}

