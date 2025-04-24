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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.overviewmap.OverviewMap

//@Composable
//fun OverviewMapForMapView(
//    viewpoint: Viewpoint?,
//    visibleArea: Polygon?,
//    symbol: Symbol = remember {
//        SimpleFillSymbol(
//            outline = SimpleLineSymbol(color = Color.red),
//            color = Color.transparent
//        )
//    },
//    scaleFactor: Double = 25.0,
//    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
//    modifier: Modifier = Modifier
//) {
//    OverviewMapImpl(
//        viewpoint = viewpoint,
//        visibleArea = visibleArea,
//        scaleFactor = scaleFactor,
//        fillSymbol = symbol,
//        map = map,
//        modifier = modifier
//    )
//}
//
//@Composable
//fun OverviewMapForSceneView(
//    viewpoint: Viewpoint?,
//    scaleFactor: Double = 25.0,
//    symbol: Symbol = remember {
//        SimpleMarkerSymbol(
//            style = SimpleMarkerSymbolStyle.Cross,
//            color = Color.red
//        )
//    },
//    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
//    modifier: Modifier = Modifier
//) {
//    OverviewMapImpl(
//        viewpoint = viewpoint,
//        scaleFactor = scaleFactor,
//        markerSymbol = symbol,
//        map = map,
//        modifier = modifier
//    )
//}
//
//@Composable
//private fun OverviewMapImpl(
//    viewpoint: Viewpoint?,
//    map: ArcGISMap,
//    visibleArea: Polygon? = null,
//    markerSymbol: Symbol? = null,
//    fillSymbol: Symbol? = null,
//    scaleFactor: Double = 25.0,
//    modifier: Modifier = Modifier,
//) {
//    val proxy = remember {
//        MapViewProxy()
//    }
//
//    val graphic = remember {
//        Graphic()
//    }
//
//    val graphicsOverlay = remember {
//        GraphicsOverlay(listOf(graphic))
//    }
//
//    val graphicsOverlays = remember {
//        listOf(graphicsOverlay)
//    }
//
//    viewpoint?.let {
//        if (viewpoint.viewpointType == ViewpointType.CenterAndScale) {
//            proxy.setViewpoint(
//                Viewpoint(
//                    center = it.targetGeometry as Point,
//                    scale = it.targetScale * scaleFactor
//                )
//            )
//            if (visibleArea == null) {
//                graphic.geometry = it.targetGeometry
//                graphic.symbol = markerSymbol
//            } else {
//                graphic.geometry = visibleArea
//                graphic.symbol = fillSymbol
//            }
//        }
//    }
//
//    MapView(
//        modifier = modifier,
//        arcGISMap = map,
//        mapViewProxy = proxy,
//        graphicsOverlays = graphicsOverlays,
//        isAttributionBarVisible = false,
//        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
//    )
//}

@Composable
fun MainScreen() {
//    val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }
//    val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

//    val arcGISMap by remember {
//        mutableStateOf(
//            ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
//                initialViewpoint = Viewpoint(
//                    latitude = 39.8,
//                    longitude = -98.6,
//                    scale = 10e7
//                )
//            }
//        )
//    }


    Column {
        Box(modifier = Modifier.weight(0.5f)) {
            val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }

            SceneView(
                modifier = Modifier.fillMaxSize(),
                arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISLightGray).apply {
                        initialViewpoint = Viewpoint(
                            latitude = 39.8,
                            longitude = -98.6,
                            scale = 10e7
                        )
                    }
                },
                onViewpointChangedForCenterAndScale = {
                    viewpoint.value = it
                },
            )
            OverviewMap(
                viewpoint = viewpoint.value,
                modifier = Modifier.size(250.dp, 200.dp).padding(20.dp).align(Alignment.TopEnd)
            )
        }

        Box(modifier = Modifier.weight(0.5f)) {
            val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }
            val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = remember {
                    ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
                        initialViewpoint = Viewpoint(
                            latitude = 39.8,
                            longitude = -98.6,
                            scale = 10e7
                        )
                    }
                },
                onViewpointChangedForCenterAndScale = {
                    viewpoint.value = it
                },
                onVisibleAreaChanged = {
                    visibleArea.value = it
                }
            )
            OverviewMap(
                viewpoint = viewpoint.value,
                visibleArea = visibleArea.value,
                modifier = Modifier.size(250.dp, 200.dp).padding(20.dp).align(Alignment.TopEnd)
            )
        }
    }
}
