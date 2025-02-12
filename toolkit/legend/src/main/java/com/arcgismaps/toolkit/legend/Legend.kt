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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.LayerContent

internal typealias LayerRow = LayerContent

@Composable
public fun Legend(
    geoModel: GeoModel,
    viewpoint: Viewpoint?,
    modifier: Modifier = Modifier
) {
    var legendItems by remember { mutableStateOf(emptyList<LayerContent>()) }

    val currentScale by remember(viewpoint) {
        mutableDoubleStateOf( viewpoint?.targetScale ?: Double.NaN)
    }

    LaunchedEffect(geoModel) {
        geoModel.load().onSuccess {
            legendItems = getGeoModelLayersInOrder(geoModel)
        }
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
    if (currentScale == 0.0 || currentScale.isNaN()) {
        return
    }
    LazyColumn(modifier = modifier) {
        items(legendItems) { item ->
            if (item.showLayer(currentScale)) {
                Row {
                    Text(text = item.name)
                }
            }
        }
    }
}

private fun LayerRow.showLayer(scale: Double): Boolean {
    return this.isVisibleAtScale(scale)
}

private fun getGeoModelLayersInOrder(geoModel: GeoModel): List<LayerRow> {
    var layerListToDisplayInLegend = mutableListOf<LayerRow>()

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

