/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.mapcomposeapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geocompose.MapOperator

private val borderPolyline by lazy {
    PolylineBuilder(SpatialReference.webMercator()) {
        addPoint(Point(-9981328.687124, 6111053.281447))
        addPoint(Point(-9946518.044066, 6102350.620682))
        addPoint(Point(-9872545.427566, 6152390.920079))
        addPoint(Point(-9838822.617103, 6157830.083057))
        addPoint(Point(-9446115.050097, 5927209.572793))
        addPoint(Point(-9430885.393759, 5876081.440801))
        addPoint(Point(-9415655.737420, 5860851.784463))
    }.toGeometry()
}

// create a red polyline graphic to cut the polygon
val polylineGraphic = Graphic(
    borderPolyline, SimpleLineSymbol(
        SimpleLineSymbolStyle.Dot,
        Color.red, 3F
    )
)

// add the polyline to the graphics overlay
private val graphicsOverlay = GraphicsOverlay().apply {
    graphics.add(polylineGraphic)
}

@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }

    // Map () will not work outside of the MapOperatorScope
    MapOperator { mapOperator ->
        Map(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
            mapOperator = mapOperator,
            onSingleTapConfirmed = { }
        ){

        }

        mapOperator.graphicsOverlays.add(graphicsOverlay)
        mapOperator.setViewpoint(Viewpoint(polylineGraphic.geometry!!))
    }
}
