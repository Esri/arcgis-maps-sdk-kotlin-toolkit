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

package com.arcgismaps.toolkit.arworldscaleapp.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.layers.ArcGISVectorTiledLayer
import com.arcgismaps.mapping.symbology.Renderer
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.ar.internal.WorldScaleCalibrationViewDefaults

@Composable
fun MainScreen() {
    val basemap = Basemap(BasemapStyle.ArcGISHumanGeography).apply {
        baseLayers.clear()

    }
    val arcGISScene = ArcGISScene(basemap).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
        // an elevation source is required for the scene to be placed at the correct elevation
        // if not used, the scene may appear far below the device position because the device position
        // is calculated with elevation
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        baseSurface.opacity = 0.3f
        // add the Esri 3D Buildings layer
//        operationalLayers.add(
//            ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=b8fec5af7dfe4866b1b8ac2d2800f282").apply {
//                this.altitudeOffset = 10.0
//            }
//        )
    }
    var displayCalibrationView by remember { mutableStateOf(false) }
    val graphicsOverlays = remember { listOf(GraphicsOverlay()) }
    val proxy = remember { WorldScaleSceneViewProxy() }

    WorldScaleSceneView(
        arcGISScene = arcGISScene,
        modifier = Modifier.fillMaxSize(),
        onInitializationStatusChanged = {
            Log.d("ArWorldScaleApp", "Initialization status changed: $it")
        },
        worldScaleSceneViewProxy = proxy,
        onSingleTapConfirmed = { singleTapConfirmedEvent ->
            proxy.screenToBaseSurface(singleTapConfirmedEvent.screenCoordinate)?.let {  point ->
                graphicsOverlays.first().graphics.add(
                    Graphic(point, SimpleMarkerSceneSymbol(SimpleMarkerSceneSymbolStyle.Diamond, Color.green, height = 6.0, width = 6.0, depth = 6.0))
                )
            }
        },
        graphicsOverlays = graphicsOverlays
    ) {
        if (displayCalibrationView) {
            CalibrationView(
                onDismiss = { displayCalibrationView = false },
                modifier = Modifier,
                colorScheme = WorldScaleCalibrationViewDefaults.colorScheme(),
                typography = WorldScaleCalibrationViewDefaults.typography()
            )
        } else {
            FloatingActionButton(onClick = { displayCalibrationView = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Calibration View button")
            }
        }
    }
}
