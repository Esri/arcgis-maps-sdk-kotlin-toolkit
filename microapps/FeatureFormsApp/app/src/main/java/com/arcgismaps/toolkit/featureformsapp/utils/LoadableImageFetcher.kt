/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureformsapp.utils

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.arcgismaps.portal.LoadableImage

/**
 * A [Fetcher] implementation that loads an image from a [LoadableImage] source
 * and returns it as an [ImageFetchResult].
 */
class LoadableImageFetcher(
    private val loadable: LoadableImage
) : Fetcher {

    /**
     * Fetches the image from the [LoadableImage] source and returns it as an [ImageFetchResult].
     *
     * @return An [ImageFetchResult] containing the loaded image as a bitmap, or null if the image could not be loaded.
     */
    override suspend fun fetch(): ImageFetchResult? {
        loadable.retryLoad()
        // Return the loaded image as an ImageFetchResult, or null if unavailable
        return loadable.image?.bitmap?.asImage()?.let { image ->
            ImageFetchResult(
                image = image,
                isSampled = false,
                dataSource = DataSource.NETWORK
            )
        }
    }

    /**
     * A [Fetcher.Factory] implementation that creates a [LoadableImageFetcher].
     */
    class Factory : Fetcher.Factory<LoadableImage> {
        override fun create(
            data: LoadableImage,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = LoadableImageFetcher(data)
    }

    /**
     * A [coil3.key.Keyer] implementation that generates a unique key for the [LoadableImage] for
     * caching purposes.
     */
    class Keyer : coil3.key.Keyer<LoadableImage> {
        override fun key(
            data: LoadableImage,
            options: Options
        ): String? = data.uri
    }
}
