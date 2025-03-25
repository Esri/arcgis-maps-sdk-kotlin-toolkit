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

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.toolkit.legend.theme.LegendDefaults
import com.arcgismaps.toolkit.legend.theme.Typography
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.parcelize.Parcelize

@Parcelize
private data class LegendItem(
    val name: String,
    val bitmap: Bitmap?
) : Parcelable

@Parcelize
private data class LayerContentData(
    val name: String,
    val legendItems: MutableList<LegendItem> = mutableListOf(),
    val isVisible: (Double) -> Boolean
) : Parcelable

@Composable
public fun Legend(
    operationalLayers: List<LayerContent>,
    basemap: Basemap?,
    currentScale: Double,
    modifier: Modifier = Modifier,
    reverseLayerOrder: Boolean = false,
    respectScaleRange: Boolean = true,
    typography: Typography = LegendDefaults.typography()
) {
    val density = LocalContext.current.resources.displayMetrics.density
    var initialized: Boolean by rememberSaveable(
        operationalLayers,
        basemap
    ) { mutableStateOf(false) }
    val layerContentData =
        rememberSaveable(operationalLayers, basemap, saver = snapshotStateListSaver()) {
            mutableStateListOf<LayerContentData>()
        }

    if (!initialized) {
        // initialize legend content once
        LaunchedEffect(Unit) {
            operationalLayers.filterIsInstance<Layer>().forEach {
                it.load().onFailure { return@LaunchedEffect }
            }
            basemap?.load()?.onFailure { return@LaunchedEffect }

            basemap?.baseLayers?.forEach { it.load().onFailure { return@LaunchedEffect } }
            basemap?.referenceLayers?.forEach { it.load().onFailure { return@LaunchedEffect } }
            legendContent(
                layerContentData,
                operationalLayers,
                basemap?.baseLayers ?: emptyList(),
                basemap?.referenceLayers ?: emptyList(),
                reverseLayerOrder,
                density
            )
            initialized = true
        }
    } else {
        // Get the operational layers' and basemaps' sublayer contents or, if there are none,
        // the basemaps' reference layers sublayer contents. If no sublayer content is available
        // just get an empty list
        LaunchedEffect(Unit) {
            // Establish the sublayerContents observation after initialization and after a config change.
            val sublayerContent = operationalLayers.map {
                it.subLayerContents
            } + (basemap?.baseLayers?.map {
                it.subLayerContents
            } ?: emptyList()) + (basemap?.referenceLayers?.map {
                it.subLayerContents
            } ?: emptyList())

            // Drop the first values -- the initial sublayer contents are already in the Legend.
            val dropList = sublayerContent.map {
                it.drop(1)
            }
            val sublayerChangedFlow: Flow<List<LayerContent>> =
                merge(*dropList.toTypedArray()).shareIn(this, SharingStarted.Lazily)

            sublayerChangedFlow
                .collect {
                    legendContent(
                        layerContentData,
                        operationalLayers,
                        basemap?.baseLayers ?: emptyList(),
                        basemap?.referenceLayers ?: emptyList(),
                        reverseLayerOrder,
                        density
                    )
                }
        }
    }

    val validScale = (currentScale != 0.0 && !currentScale.isNaN())
    if (validScale && initialized) {
        Legend(modifier, layerContentData, currentScale, respectScaleRange, typography)
    } else {
        CircularProgressIndicator()
    }
}


@Composable
private fun Legend(
    modifier: Modifier,
    legendItems: List<LayerContentData>,
    currentScale: Double,
    respectScaleRange: Boolean,
    typography: Typography
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(legendItems) { index, item ->
            if (!respectScaleRange || item.isVisible(currentScale)) {
                if (index == legendItems.size - 1 || item.name != legendItems[index + 1].name) {
                    Row {
                        Text(text = item.name, style = typography.layerName)
                    }
                }
                if (item.legendItems.isNotEmpty()) {
                    item.legendItems.forEach { legendInfo ->
                        LegendInfoRow(legendInfo, typography)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendInfoRow(
    legendInfo: LegendItem,
    typography: Typography,
) {
    Row {
        legendInfo.bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.symbol_description)
            )
        }
        Text(text = legendInfo.name, style = typography.legendInfoName)
    }
}

/**
 * Provides the row data for LayerContent and their sublayer content.
 *
 * @param layerContentData the output list. It is a SnapshotStateList which when altered will cause recomposition.
 * @param layers the operational layers of the map
 * @param baseLayers the baseLayers of the Basemap
 * @param referenceLayers the referenceLayers of the Basemap
 * @param reverseOrder build the list in reverse order when true
 * @param density the screen metrics used to create symbol swatches for the Symbols returned by
 * LayerContent.fetchLegendInfos()
 *
 * @since 200.7.0
 */
private suspend fun legendContent(
    layerContentData: MutableList<LayerContentData>,
    layers: List<LayerContent>,
    baseLayers: List<LayerContent>,
    referenceLayers: List<LayerContent>,
    reverseOrder: Boolean,
    density: Float
) {
    layerContentData.clear()
    // Add the layers to the layer content data
    // Add the operational layers first
    addLayersAndSubLayersDataToLayerContentData(
        layers,
        reverseOrder,
        density,
        layerContentData
    )
    // Add the basemap layers
    addLayersAndSubLayersDataToLayerContentData(
        baseLayers,
        reverseOrder,
        density,
        layerContentData,
        addAtIndexZero = reverseOrder
    )
    // Add the reference layers
    addLayersAndSubLayersDataToLayerContentData(
        referenceLayers,
        reverseOrder,
        density,
        layerContentData,
        addAtIndexZero = !reverseOrder
    )
}

/**
 * Provides LayerContentData for a list of LayerContent. Descends recursively into any sublayer content
 *
 * @param layers the LayerContents to evaluate
 * @param reverseLayerOrder build the LegendContent in reverse order when trye
 * @param density the screen metrics to use to create bitmaps from Symbol swatches
 * @param layerContentData the output list of LayerContentData
 * @param addAtIndexZero used in reverse ordering to build the overall list for operational, base,
 * and reference layers
 *
 * @since 200.7.0
 */
private suspend fun addLayersAndSubLayersDataToLayerContentData(
    layers: List<LayerContent>?,
    reverseLayerOrder: Boolean,
    density: Float,
    layerContentData: MutableList<LayerContentData>,
    addAtIndexZero: Boolean = false
) {
    layers?.let { layerList ->
        // The order of the layers is reversed to match the order in the legend
        val orderedLayers = if (reverseLayerOrder) layerList else layerList.reversed()
        val filteredLayers = orderedLayers.filter { it.isVisible && it.showInLegend }
        val layersAndSubLayersLayerContentData =
            filteredLayers.flatMap { getLayerContentData(it, density) }
        if (addAtIndexZero) {
            layerContentData.addAll(0, layersAndSubLayersLayerContentData)
        } else {
            layerContentData.addAll(layersAndSubLayersLayerContentData)
        }
    }
}

/**
 * Gets the LayerContentData for a single LayerContent. Descends recursively into any sublayerContents
 *
 * @param layerContent the LayerContent to evaluate
 * @param density the screen metrics density to use to create bitmaps from Symbol swatches
 * @return a list of LayerContentData representing the LayerContent in addition to any
 * LayerContent.sublayerContents
 * @since 200.7.0
 */
private suspend fun getLayerContentData(
    layerContent: LayerContent,
    density: Float
): List<LayerContentData> {
    val data = LayerContentData(layerContent.name) { scale ->
        layerContent.isVisibleAtScale(scale)
    }
    layerContent.fetchLegendInfos().onSuccess {
        it.map { info ->
            val bitmap = info.symbol?.createSwatch(density)?.getOrNull()?.bitmap
            LegendItem(info.name, bitmap)
        }.also { items ->
            data.legendItems.addAll(items)
        }
    }
    return if (layerContent.subLayerContents.value.isEmpty()) {
        listOf(data)
    } else {
        listOf(data) + layerContent.subLayerContents.value.flatMap { sublayer ->
            getLayerContentData(sublayer, density)
        }
    }
}

/**
 * Preserves a SnapshotStateList across compositions
 *
 * @since 200.7.0
 */
private fun <T : Parcelable> snapshotStateListSaver(): Saver<SnapshotStateList<T>, Any> = listSaver(
    {
        it.toList()
    },
    {
        mutableStateListOf<T>().apply {
            addAll(it)
        }
    }
)
