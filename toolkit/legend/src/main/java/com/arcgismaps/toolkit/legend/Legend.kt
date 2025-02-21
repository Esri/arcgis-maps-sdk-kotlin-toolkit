/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.legend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import coil.util.DebugLogger
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.mapping.layers.LegendInfo
import com.arcgismaps.mapping.symbology.Symbol
import okio.Buffer
import java.io.ByteArrayOutputStream

@Immutable
internal data class LayerRow (
    val layer: LayerContent,
    val isVisibleAtScale: (Double) -> Boolean,
    val legendInfos: List<LegendInfo>
)

@Composable
public fun Legend(
    geoModel: GeoModel,
    currentScale: Double,
    modifier: Modifier = Modifier
) {
    var legendItems by rememberSaveable(geoModel) { mutableStateOf(emptyList<LayerRow>()) }

    if (legendItems.isEmpty()) {
        LaunchedEffect(geoModel) {
            geoModel.load().onSuccess {
                val geoModelLayers = getGeoModelLayersInOrder(geoModel)
                loadAndGetAllLayerRows(geoModelLayers) { loadedLayerRows ->
                    legendItems = loadedLayerRows
                }
            }
        }
    }
    if (currentScale == 0.0 || currentScale.isNaN()) {
        return
    }

    if (legendItems.isNotEmpty()) {
        Legend(modifier, legendItems, currentScale)
    }
}

@Composable
private fun Legend(
    modifier: Modifier,
    legendItems: List<LayerRow>,
    currentScale: Double,
    ) {
    LazyColumn(modifier = modifier) {
        items(legendItems) { item ->
            if (item.isVisibleAtScale(currentScale)) {
                Row {
                    Text(text = item.layer.name)
                }
                if (item.legendInfos.isNotEmpty()) {
                    item.legendInfos.forEach { legendInfo ->
                        LegendInfoRow(legendInfo, LocalContext.current)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendInfoRow(legendInfo: LegendInfo, context: Context) {
    Row {
        val symbol = legendInfo.symbol
        if (symbol != null) {
            val imageFetcher = remember { SymbolImageFetcher.Factory(symbol, context) }
            val imageLoader = rememberImageLoader(context)
            val image = remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(legendInfo) {
                val request = ImageRequest.Builder(context)
                    .data(symbol)
                    .fetcherFactory(imageFetcher)
                    .build()
                val result = imageLoader.execute(request)
                image.value = (result.drawable as? BitmapDrawable)?.bitmap
            }

            image.value?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null
                )
            }
        }
        Text(text = legendInfo.name)
    }
}

private fun getGeoModelLayersInOrder(geoModel: GeoModel): List<LayerContent> {
    var layerListToDisplayInLegend = mutableListOf<LayerContent>()

    // add all operational layers
    geoModel.operationalLayers.let { layerListToDisplayInLegend.addAll(it) }

    val basemap = geoModel.basemap.value
    basemap?.let { it ->
        it.referenceLayers.let { layerListToDisplayInLegend.addAll(it) }
        it.baseLayers.let { layerListToDisplayInLegend.addAll(0, it) }
    }

    layerListToDisplayInLegend = layerListToDisplayInLegend.filter { it.isVisible && it.showInLegend }.toMutableList()

    return layerListToDisplayInLegend.reversed()
}

/**
 * Loads all the layers and sublayers in the GeoModel.
 * Returns a list of LayerRow objects in the onComplete lambda, which are the layers and sublayers of
 * the GeoModel in order.
 *
 * @param geoModel The GeoModel to load the layers and sublayers from.
 * @param onComplete The callback to execute when the layers and sublayers are loaded.
 */
private suspend fun loadAndGetAllLayerRows(
    geoModelLayersInOrder: List<LayerContent>,
    onComplete: (List<LayerRow>) -> Unit
) {
    val layerRows = geoModelLayersInOrder.flatMap { layerContent ->
        loadLayerRow(layerContent)
    }
    onComplete(layerRows)
}

/**
 * Loads the layer and its sublayers.
 *
 * @param layerContent The layer to load.
 * @return A list of LayerRow objects.
 */
private suspend fun loadLayerRow(layerContent: LayerContent): List<LayerRow> {
    return if (layerContent is Layer) {
        val result = layerContent.load()
        if (result.isSuccess) {
            fetchLayerRowsWithSublayersAndLegendInfos(layerContent)
        } else {
            emptyList()
        }
    } else {
        fetchLayerRowsWithSublayersAndLegendInfos(layerContent)
    }
}

/**
 * Fetches the layer's sublayers and legend infos.
 *
 * @param layerContent The layer to fetch the sublayers and legend infos from.
 * @return A list of LayerRow objects.
 */
private suspend fun fetchLayerRowsWithSublayersAndLegendInfos(layerContent: LayerContent): List<LayerRow> {
    val layerRows = mutableListOf<LayerRow>()
    if (layerContent.subLayerContents.value.isNotEmpty()) {
        layerContent.subLayerContents.value.forEach { subLayer ->
            layerRows.addAll(loadLayerRow(subLayer))
        }
    } else {
        layerContent.fetchLegendInfos().onSuccess { legendInfos ->
            layerRows.add(
                LayerRow(
                    layerContent,
                    { scale ->
                        layerContent.isVisibleAtScale(
                            scale
                        )
                    },
                    legendInfos
                )
            )
        }
    }
    return layerRows
}

@Composable
private fun rememberImageLoader(context: Context): ImageLoader {
    return remember {
        ImageLoader(context).newBuilder()
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir)
                    .maxSizePercent(.03)
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }
}

internal class SymbolImageFetcher(
    private val symbol: Symbol,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val response =
            symbol.createSwatch(context.resources.displayMetrics.density).getOrNull()?.bitmap

        val bos = ByteArrayOutputStream()
        val buffer = Buffer()
        bos.use {
            response?.compress(Bitmap.CompressFormat.PNG, 100, bos)
            buffer.write(bos.toByteArray())
        }
        return SourceResult(
            source = ImageSource(buffer, context),
            dataSource = DataSource.MEMORY,
            mimeType = null
        )
    }

    class Factory(
        private val symbol: Symbol,
        private val context: Context
    ) : Fetcher.Factory<Symbol> {
        override fun create(data: Symbol, options: Options, imageLoader: ImageLoader): Fetcher =
            SymbolImageFetcher(symbol, context)
    }
}

