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
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.legend.Legend

@Composable
fun MainScreen(modifier: Modifier) {
    val portalItem =
//        PortalItem(Portal("https://www.arcgis.com"), "f1ed0d220d6447a586203675ed5ac213") // dot net
//        PortalItem(Portal("https://www.arcgis.com"), "16f1b8ba37b44dc3884afc8d5f454dd2") // dot net
//        PortalItem(Portal("https://www.arcgis.com"), "1966ef409a344d089b001df85332608f") // mark iOS
        PortalItem(Portal("https://www.arcgis.com"), "5588bd6cf0484b1a8fb92b0d8478a386") // San Diego census
//    val arcGISMap by remember {
//        mutableStateOf(
//            ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
//                initialViewpoint = Viewpoint(
//                    latitude = 39.8,
//                    longitude = -98.6,
//                    scale = 10e7
//                )
//            }
//        )
//    }
    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(portalItem)
//                .apply {
//                initialViewpoint = Viewpoint(
//                    latitude = 39.8,
//                    longitude = -98.6,
//                    scale = 10e7
//                )
//            }
        )
    }

    var viewpoint: Viewpoint? by remember { mutableStateOf(null) }
    var geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
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
            geoModel = arcGISMap,
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
                arcGISMap.operationalLayers.add(tiledLayer)
            },
        ) {
            Text("Add layer")
        }
    }
}
