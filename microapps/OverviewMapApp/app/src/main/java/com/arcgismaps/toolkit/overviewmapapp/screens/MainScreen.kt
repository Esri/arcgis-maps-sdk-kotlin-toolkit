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
import androidx.compose.foundation.layout.padding
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
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneView

@Composable
fun OverviewMapForMapView(
    viewpoint: Viewpoint?,
    visibleArea: Polygon?,
    symbol: Symbol = remember {
        SimpleFillSymbol(
            outline = SimpleLineSymbol(color = Color.red),
            color = Color.transparent
        )
    },
    scaleFactor: Double = 25.0,
    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    modifier: Modifier = Modifier
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        visibleArea = visibleArea,
        scaleFactor = scaleFactor,
        fillSymbol = symbol,
        map = map,
        modifier = modifier
    )
}

@Composable
fun OverviewMapForSceneView(
    viewpoint: Viewpoint?,
    scaleFactor: Double = 25.0,
    symbol: Symbol = remember {
        SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.red
        )
    },
    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    modifier: Modifier = Modifier
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        scaleFactor = scaleFactor,
        markerSymbol = symbol,
        map = map,
        modifier = modifier
    )
}

//@Composable
//fun OverviewMap(
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
//    modifier: Modifier = Modifier,
//) {
//    OverviewMapImpl(
//        viewpoint = viewpoint,
//        visibleArea = visibleArea,
//        scaleFactor = scaleFactor,
//        map = map,
//        fillSymbol = symbol,
//        markerSymbol = null,
//        modifier = modifier,
//    )
////    val proxy = remember {
////        MapViewProxy()
////    }
////
////    val graphic = remember {
////        Graphic().apply {
////            symbol = SimpleFillSymbol(
////                outline = SimpleLineSymbol(color = Color.red),
////                color = Color.transparent
////            )
////        }
////    }
////
////    val graphicsOverlay = remember {
////        GraphicsOverlay(listOf(graphic))
////    }
////
////    val graphicsOverlays = remember {
////        listOf(graphicsOverlay)
////    }
////
////    viewpoint?.let {
////        if (it.viewpointType == ViewpointType.CenterAndScale) {
////            val scaledViewpoint =
////                Viewpoint(
////                    center = it.targetGeometry as Point,
////                    scale = it.targetScale * scaleFactor
////                )
////            proxy.setViewpoint(scaledViewpoint)
////        }
////    }
////
////    visibleArea?.let {
////        graphic.geometry = it
////    }
////
////    MapView(
////        modifier = modifier,
////        arcGISMap = map,
////        mapViewProxy = proxy,
////        graphicsOverlays = graphicsOverlays,
////        isAttributionBarVisible = false,
////        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
////    )
//}

//@Composable
//fun OverviewMap(
//    viewpoint: Viewpoint?,
//    scaleFactor: Double = 25.0,
//    symbol: Symbol = remember {
//        SimpleMarkerSymbol(
//            style = SimpleMarkerSymbolStyle.Cross,
//            color = Color.red
//        )
//    },
//    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
//    modifier: Modifier = Modifier,
//) {
//    OverviewMapImpl(
//        viewpoint = viewpoint,
//        visibleArea = null,
//        scaleFactor = scaleFactor,
//        map = map,
//        fillSymbol = null,
//        markerSymbol = symbol,
//        modifier = modifier
//    )
////    val proxy = remember {
////        MapViewProxy()
////    }
////
////    val graphic = remember {
////        Graphic().apply {
////            symbol = SimpleMarkerSymbol(style = SimpleMarkerSymbolStyle.Cross, color = Color.red)
////        }
////    }
////
////    val graphicsOverlay = remember {
////        GraphicsOverlay(listOf(graphic))
////    }
////
////    val graphicsOverlays = remember {
////        listOf(graphicsOverlay)
////    }
////
////    MapView(
////        modifier = modifier,
////        arcGISMap = map,
////        mapViewProxy = proxy,
////        graphicsOverlays = graphicsOverlays,
////        isAttributionBarVisible = false,
////        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
////    )
////
////    viewpoint?.let {
////        if (it.viewpointType == ViewpointType.CenterAndScale) {
////            val scaledViewpoint = Viewpoint(
////                center = it.targetGeometry as Point,
////                scale = it.targetScale * scaleFactor
////            )
////
////            proxy.setViewpoint(scaledViewpoint)
////            graphic.geometry = it.targetGeometry
////        }
////    }
////
////    MapView(
////        modifier = modifier,
////        arcGISMap = map,
////        mapViewProxy = proxy,
////        graphicsOverlays = graphicsOverlays,
////        isAttributionBarVisible = false,
////        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
////    )
//}

@Composable
private fun OverviewMapImpl(
    viewpoint: Viewpoint?,
    map: ArcGISMap,
    visibleArea: Polygon? = null,
    markerSymbol: Symbol? = null,
    fillSymbol: Symbol? = null,
    scaleFactor: Double = 25.0,
    modifier: Modifier = Modifier,
) {
    val proxy = remember {
        MapViewProxy()
    }

    val graphic = remember {
        Graphic()
    }

    val graphicsOverlay = remember {
        GraphicsOverlay(listOf(graphic))
    }

    val graphicsOverlays = remember {
        listOf(graphicsOverlay)
    }

    viewpoint?.let {
        if (viewpoint.viewpointType == ViewpointType.CenterAndScale) {
            proxy.setViewpoint(
                Viewpoint(
                    center = it.targetGeometry as Point,
                    scale = it.targetScale * scaleFactor
                )
            )
            if (visibleArea == null) {
                graphic.geometry = it.targetGeometry
                graphic.symbol = markerSymbol
            } else {
                graphic.geometry = visibleArea
                graphic.symbol = fillSymbol
            }
        }
    }

    MapView(
        modifier = modifier,
        arcGISMap = map,
        mapViewProxy = proxy,
        graphicsOverlays = graphicsOverlays,
        isAttributionBarVisible = false,
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
    )
}

@Composable
fun MainScreen() {
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

//    SceneView(
//        modifier = Modifier.fillMaxSize(),
//        arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISDarkGray) },
//        onViewpointChangedForCenterAndScale = {
//            viewpoint.value = it
//        },
//    )

    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = arcGISMap,
        onViewpointChangedForCenterAndScale = {
            viewpoint.value = it
        },
        onVisibleAreaChanged = {
            visibleArea.value = it
        }
    )

//    OverviewMapForSceneView(
//        viewpoint = viewpoint.value,
//        symbol = remember { SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 30.0f) },
//        modifier = Modifier.padding(20.dp).size(250.dp, 200.dp),
//    )

    OverviewMapForMapView(
        viewpoint = viewpoint.value,
        visibleArea = visibleArea.value,
        modifier = Modifier.size(250.dp, 200.dp).padding(20.dp)
    )
}
