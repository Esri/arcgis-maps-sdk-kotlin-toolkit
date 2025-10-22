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

package com.arcgismaps.toolkit.featureforms.internal.utils

import android.content.Context
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.arcgismaps.mapping.symbology.Symbol

/**
 * A shared [ImageLoader] instance for loading images throughout the toolkit module.
 *
 * Must be initialized by calling [init] before use. If not initialized, it will be lazily
 * initialized with the needed settings on first use.
 */
internal object SharedImageLoader {

    @Volatile
    private var loader: ImageLoader? = null

    /**
     * Initializes the shared [ImageLoader] with needed settings. This method is thread-safe.
     *
     * @param context A [Context] used to build the [ImageLoader].
     */
    fun init(context: Context) {
        if (loader == null) {
            synchronized(this) {
                if (loader == null) {
                    loader = buildDefault(context)
                }
            }
        }
    }

    /**
     * Gets the shared [ImageLoader] instance. If it has not been initialized by calling [init],
     * it will be lazily initialized with needed settings.
     */
    fun get(context: Context): ImageLoader {
        // Lazy init if host forgot to call init()
        return loader ?: synchronized(this) {
            buildDefault(context).also { loader = it }
        }
    }

    /**
     * Builds a default [ImageLoader] instance with the needed configuration.
     */
    private fun buildDefault(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SymbolFetcher.Factory(context.resources.displayMetrics.density))
                add(SymbolFetcher.Keyer())
            }
            .build()
    }
}

/**
 * A [Fetcher] implementation for loading images from an [Symbol].
 *
 * This fetcher uses the symbol's `createSwatch` method to generate a drawable representation
 * of the symbol, which is then converted to an image that can be used by Coil.
 *
 * @param symbol The [Symbol] to fetch the image for.
 * @param density The screen density to use when creating the symbol swatch.
 */
private class SymbolFetcher(
    private val symbol: Symbol,
    private val density: Float
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return symbol.createSwatch(density).getOrNull()?.let { drawable ->
            ImageFetchResult(
                image = drawable.bitmap.asImage(),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }
    }

    /**
     * A [Fetcher.Factory] implementation for creating [SymbolFetcher] instances.
     *
     * @param density The screen density to use when creating symbol swatches.
     */
    class Factory(private val density: Float) : Fetcher.Factory<Symbol> {
        override fun create(
            data: Symbol,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = SymbolFetcher(data, density)
    }

    /**
     * A [coil3.key.Keyer] implementation for generating cache keys for [Symbol] objects.
     */
    class Keyer : coil3.key.Keyer<Symbol> {
        override fun key(
            data: Symbol,
            options: Options
        ): String? = data.toJson()
    }
}
