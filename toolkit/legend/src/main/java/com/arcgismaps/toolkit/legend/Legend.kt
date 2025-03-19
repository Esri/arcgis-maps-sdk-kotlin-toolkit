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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import kotlinx.parcelize.Parcelize

@Parcelize
private data class LegendItem(
    val name: String,
    val bitmap: Bitmap?
): Parcelable

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
    respectScaleRange: Boolean = true,
    modifier: Modifier = Modifier
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

            layerContentData.addAll(
                operationalLayers.reversed().filter { it.isVisible && it.showInLegend }.flatMap {
                    layerContentData(it, density)
                }
            )

            basemap?.referenceLayers?.let { refLayers ->
                layerContentData.addAll(0,
                    refLayers.filter { it.isVisible && it.showInLegend }.flatMap {
                        layerContentData(it, density)
                    }
                )
            }

            basemap?.baseLayers?.let { baseLayers ->
                layerContentData.addAll(
                    baseLayers.filter { it.isVisible && it.showInLegend }.flatMap {
                        layerContentData(it, density)
                    }
                )
            }

            initialized = true
        }
    }

    if (currentScale == 0.0 || currentScale.isNaN()) {
        return
    }

    if (initialized) {
        Legend(modifier, layerContentData, currentScale, respectScaleRange)
    } else {
        CircularProgressIndicator()
    }
}

private suspend fun layerContentData(
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
            layerContentData(sublayer, density)
        }
    }
}

@Composable
private fun Legend(
    modifier: Modifier,
    legendItems: List<LayerContentData>,
    currentScale: Double,
    respectScaleRange: Boolean,
    ) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(legendItems) { index, item ->
            if (!respectScaleRange || item.isVisible(currentScale)) {
                if (index == legendItems.size - 1 || item.name != legendItems[index + 1].name) {
                    Row {
                        Text(text = item.name)
                    }
                }
                if (item.legendItems.isNotEmpty()) {
                    item.legendItems.forEach { legendInfo ->
                        LegendInfoRow(legendInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendInfoRow(
    legendInfo: LegendItem,
) {
    Row {
        legendInfo.bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.symbol_description)
            )
        }
        Text(text = legendInfo.name)
    }
}
