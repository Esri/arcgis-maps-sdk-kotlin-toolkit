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

package com.arcgismaps.toolkit.tabletoparapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewProxy
import kotlin.math.roundToInt

@Composable
fun MainScreen() {
    val arcGISScene = remember {
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(34.056295, -117.195800, 10000000.0)
        }
    }
    val tableTopSceneViewProxy = remember { TableTopSceneViewProxy() }
    var tappedLocation by remember { mutableStateOf<Point?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        TableTopSceneView(
            arcGISScene = arcGISScene,
            modifier = Modifier.fillMaxSize(),
            tableTopSceneViewProxy = tableTopSceneViewProxy,
            onSingleTapConfirmed = {
                val location = tableTopSceneViewProxy.screenToBaseSurface(it.screenCoordinate)
                location?.let { point ->
                    tappedLocation = point
                }
            }
        ) {
            tappedLocation?.let {
                Callout(location = it, modifier = Modifier.wrapContentSize()) {
                    Text("Lat: ${it.y.roundToInt()}, Lon: ${it.x.roundToInt()}")
                }
            }
        }
    }
}
