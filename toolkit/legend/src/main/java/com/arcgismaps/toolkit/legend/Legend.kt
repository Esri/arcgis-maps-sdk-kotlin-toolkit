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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.toolkit.legend.theme.LegendDefaults
import com.arcgismaps.toolkit.legend.theme.Typography
import kotlinx.parcelize.Parcelize

@Parcelize
private data class LegendItem(
    val name: String,
    val bitmap: Bitmap?
): Parcelable

@Parcelize
private data class LayerContentData(
    val name: String,
    val isLayer: Boolean,
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
    title: String = stringResource(R.string.title),
    typography: Typography = LegendDefaults.typography()
) {
    val density = LocalContext.current.resources.displayMetrics.density
    var initialized: Boolean by rememberSaveable(operationalLayers, basemap) { mutableStateOf(false) }
    val layerContentData = rememberSaveable(operationalLayers, basemap) { mutableListOf<LayerContentData>() }

    if (!initialized) {
        LaunchedEffect(Unit) {
            operationalLayers.filterIsInstance<Layer>().forEach {
                it.load().onFailure { return@LaunchedEffect }
            }
            basemap?.load()?.onFailure { return@LaunchedEffect }

            basemap?.baseLayers?.forEach { it.load().onFailure { return@LaunchedEffect } }
            basemap?.referenceLayers?.forEach { it.load().onFailure { return@LaunchedEffect } }

            // Add the layers to the layer content data
            // Add the operational layers first
            addLayersAndSubLayersDataToLayerContentData(operationalLayers, reverseLayerOrder, density, layerContentData)
            // Add the basemap layers
            addLayersAndSubLayersDataToLayerContentData(basemap?.baseLayers, reverseLayerOrder, density, layerContentData, addAtIndexZero = reverseLayerOrder)
            // Add the reference layers
            addLayersAndSubLayersDataToLayerContentData(basemap?.referenceLayers, reverseLayerOrder, density, layerContentData, addAtIndexZero = !reverseLayerOrder)

            initialized = true
        }
    }

    if (currentScale == 0.0 || currentScale.isNaN()) {
        return
    }

    if (initialized) {
        Legend(modifier, layerContentData, currentScale, respectScaleRange, title, typography)
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

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
        val layersAndSubLayersLayerContentData = filteredLayers.flatMap { getLayerContentData(it, density) }
        if (addAtIndexZero) {
            layerContentData.addAll(0, layersAndSubLayersLayerContentData)
        } else {
            layerContentData.addAll(layersAndSubLayersLayerContentData)
        }
    }
}

private suspend fun getLayerContentData(
    layerContent: LayerContent,
    density: Float
): List<LayerContentData> {
    val data = LayerContentData(layerContent.name, layerContent is Layer) { scale ->
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

@Composable
private fun Legend(
    modifier: Modifier,
    legendItems: List<LayerContentData>,
    currentScale: Double,
    respectScaleRange: Boolean,
    title: String,
    typography: Typography
    ) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = typography.title
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            itemsIndexed(legendItems) { index, item ->
                if (!respectScaleRange || item.isVisible(currentScale)) {
                    if (index == legendItems.size - 1 || item.name != legendItems[index + 1].name) {
                        Row(
                            modifier = Modifier.then(
                                if (item.isLayer) Modifier else Modifier.padding(horizontal = 10.dp)
                            )
                        ) {
                            Text(
                                text = item.name,
                                style = if (item.isLayer) typography.layerName else typography.subLayerName
                            )
                        }

                    }
                    if (item.legendItems.isNotEmpty()) {
                        item.legendItems.forEach { legendInfo ->
                            LegendInfoRow(
                                legendInfo = legendInfo,
                                typography = typography,
                                modifier = if (item.isLayer) Modifier.padding(
                                    horizontal = 5.dp,
                                    vertical = 2.dp
                                ) else Modifier.padding(
                                    horizontal = 15.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
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
    modifier: Modifier
) {
    Row(modifier = modifier) {
        legendInfo.bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.symbol_description)
            )
        }
        Text(text = legendInfo.name, style = typography.legendInfoName)
    }
}
