/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.buildingexplorerapp.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.toolkit.buildingexplorer.BuildingExplorer
import com.arcgismaps.toolkit.buildingexplorer.BuildingExplorerState

@Composable
fun MainScreen() {
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
//    MapView(
//        modifier = Modifier.fillMaxSize(),
//        arcGISMap = arcGISMap
//    )
    val buildingSceneLayer =
        BuildingSceneLayer("https://tiles.arcgis.com/tiles/V6ZHFr6zdgNZuVG0/arcgis/rest/services/BSL__4326__US_Redlands__EsriAdminBldg_PublicDemo/SceneServer")
    BuildingExplorer(
        BuildingExplorerState(
            buildingSceneLayer = buildingSceneLayer,
            coroutineScope = rememberCoroutineScope()
        )
    )
}
