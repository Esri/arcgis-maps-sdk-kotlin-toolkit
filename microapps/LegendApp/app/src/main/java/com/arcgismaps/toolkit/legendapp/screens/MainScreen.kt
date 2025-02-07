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

package com.arcgismaps.toolkit.legendapp.screens

import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.legend.Legend
import com.arcgismaps.toolkit.legend.LegendState

@Composable
fun MainScreen(modifier: Modifier) {

    var viewpoint: Viewpoint? by remember { mutableStateOf(null) }
    var geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged? by remember { mutableStateOf(null) }
    val legendViewModel: LegendViewModel = viewModel()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = legendViewModel.arcGISMap,
            onMapScaleChanged = {
                Log.e("MainScreen **", "onMapScaleChanged: $it" )
            },
            onLayerViewStateChanged = {
                geoViewLayerViewStateChanged = it
                Log.e("MainScreen **", "onLayerViewStateChanged: layername: ${it.layer.name} ${it.layerViewState.status.size}" )
                for (status in it.layerViewState.status) {
                    Log.e("MainScreen **", "status: $status")
                }
                                      },
            onViewpointChangedForCenterAndScale = {
                viewpoint = it
                Log.e("MainScreen **", "onViewpointChangedForCenterAndScale" )
            }
        )

        Legend(
            legendState = legendViewModel.legendState,
            geoViewLayerViewStateChanged = geoViewLayerViewStateChanged,
//            viewPoint = viewpoint,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        Button(
            onClick = {
                Log.e("MainScreen **", "added tiled layer")
                val tiledLayer = ArcGISTiledLayer("https://services.arcgisonline.com/arcgis/rest/services/World_Street_Map/MapServer")
                legendViewModel.arcGISMap.operationalLayers.add(tiledLayer)
            },
        ) {
            Text("Add layer")
        }
    }
}
