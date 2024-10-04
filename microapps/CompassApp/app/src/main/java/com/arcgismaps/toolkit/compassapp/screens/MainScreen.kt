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

package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier) {
    // create an ArcGISMap with a Topographic basemap style
    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                // set the map's viewpoint to North America
                initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            }
        )
    }
    var mapRotation by remember { mutableDoubleStateOf(0.0) }
    val mapViewProxy = remember { MapViewProxy() }
    val graphicsOverlay = remember { GraphicsOverlay() }
    var graphic1 : Graphic? = null
    var graphic2 : Graphic? = null
    // show composable MapView with compass
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            arcGISMap,
            modifier = Modifier.fillMaxSize(),
            graphicsOverlays = listOf(graphicsOverlay),
            mapViewProxy = mapViewProxy,
            onMapRotationChanged = { rotation -> mapRotation = rotation },
            onSingleTapConfirmed = {

                it.mapPoint?.let { point ->
                    graphic1 = Graphic(
                        geometry = point,
                        symbol = SimpleMarkerSymbol(
                            SimpleMarkerSymbolStyle.Cross,
                            Color.green,
                            20.0f
                        )
                    )
                    graphic2 = Graphic(
                        geometry = Point(
                            (point.x + 500.0),
                            point.y,
                        ),
                        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 20.0f)
                    )
                    graphicsOverlay.graphics.add(graphic1!!)
                    graphicsOverlay.graphics.add(graphic2!!)
                }
            }
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
                .padding(25.dp)
        ) {
            val coroutineScope = rememberCoroutineScope()
            // show the compass and pass the mapRotation state data
            Compass(rotation = mapRotation) {
                // reset the Composable MapView viewpoint rotation to point north
                coroutineScope.launch {
                    mapViewProxy.setViewpointRotation(0.0)
                }
            }
        }
        Row {
            // Add two buttons
            Button(onClick = { /* Handle first button click */
                graphic1?.isSelected = true
                graphic2?.isSelected = false
            }) {
                Text("select")
            }
            Button(onClick = { /* Handle second button click */
                graphic1?.isSelected = false
                graphic2?.isSelected = true
            }) {
                Text("unselect")
            }
        }
    }
}
