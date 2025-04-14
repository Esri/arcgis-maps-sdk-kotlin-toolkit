/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.overviewmapapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

@Composable
fun OverviewMap(
    viewpoint: Viewpoint?,
    visibleArea: Polygon?,
    scaleFactor: Double = 25.0,
    modifier: Modifier = Modifier,
    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
) {
//    val arcGISMap = remember {
//        map ?: ArcGISMap(BasemapStyle.ArcGISTopographic)
//    }

    val proxy = remember {
        MapViewProxy()
    }

    val graphic = remember {
        Graphic().apply {
            symbol = SimpleLineSymbol(color = Color.red)
        }
    }

    val graphicsOverlay = remember {
        GraphicsOverlay(listOf(graphic))
    }

    val graphicsOverlays = remember {
        listOf(graphicsOverlay)
    }

    MapView(
        modifier = modifier,
        arcGISMap = map,
        mapViewProxy = proxy,
        graphicsOverlays = graphicsOverlays,
        isAttributionBarVisible = false,
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
    )

    viewpoint?.let {
        proxy.setViewpoint(
            Viewpoint(
                center = viewpoint.targetGeometry as Point,
                scale = viewpoint.targetScale * scaleFactor
            )
        )
    }

    graphic.geometry = visibleArea
}

@Composable
fun MainScreen() {

//    val proxy = MapViewProxy()
//
//    val graphic = remember {
//        Graphic().apply {
//            symbol = SimpleLineSymbol(color = Color.red)
//        }
//    }
//    val graphicsOverlay = remember {
//        GraphicsOverlay(listOf(graphic))
//    }
//
//    val graphicsOverlays = remember {
//        listOf(graphicsOverlay)
//    }

    val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }
    val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
                initialViewpoint = Viewpoint(
                    latitude = 39.8,
                    longitude = -98.6,
                    scale = 10e7
                )
            }
        )
    }

//    val arcGISMap2 by remember {
//        mutableStateOf(
//            ArcGISMap(BasemapStyle.ArcGISDarkGray)
//        )
//    }

    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = arcGISMap,
        onViewpointChangedForCenterAndScale = {
            viewpoint.value = it
//            proxy.setViewpoint(
//                Viewpoint(
//                    center = it.targetGeometry as Point,
//                    scale = it.targetScale * 25.0
//                )
//            )
        },
        onVisibleAreaChanged = {
            visibleArea.value = it
            //graphic.geometry = it
        }
    )
//    MapView(
//        modifier = Modifier.size(200.dp),
//        arcGISMap = arcGISMap2,
//        mapViewProxy = proxy,
//        graphicsOverlays = graphicsOverlays,
//        isAttributionBarVisible = false
//    )
    OverviewMap(
        viewpoint = viewpoint.value,
        visibleArea = visibleArea.value,
        modifier = Modifier.size(200.dp)
    )
}
