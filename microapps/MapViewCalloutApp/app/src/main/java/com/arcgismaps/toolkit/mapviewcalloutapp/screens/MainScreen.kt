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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle

@Composable
fun MainScreen(viewModel: MapViewModel) {

    val graphicsOverlay = GraphicsOverlay()
    val graphicsOverlays by remember {
        mutableStateOf(
            listOf(
                graphicsOverlay
            )
        )
    }

    val mapPoint = viewModel.mapPoint.collectAsState().value

    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = viewModel.arcGISMap,
        graphicsOverlays = graphicsOverlays,
        onSingleTapConfirmed = {
            graphicsOverlay.graphics.clear()
            viewModel.setMapPoint(it)
            val simpleGraphic =
                Graphic(
                    it.mapPoint,
//                    mapPoint,
                    SimpleMarkerSymbol(
                        style = SimpleMarkerSymbolStyle.Circle,
                        color = com.arcgismaps.Color.black
                    )
                )
            graphicsOverlay.graphics.add(simpleGraphic)
        },
        content = if (mapPoint != null) {
            {
//                Callout(location = mapPoint, offset = DoubleXY(200.0, 0.0), rotateOffsetWithGeoView = true){
                Callout(location = mapPoint, offset = DpOffset(200.0D, 0.0), rotateOffsetWithGeoView = true){
                    Text(
                        "Hello, World!",
                        color = Color.Green
                    )
                }
            }
        } else {
            null
        }
    )
}
