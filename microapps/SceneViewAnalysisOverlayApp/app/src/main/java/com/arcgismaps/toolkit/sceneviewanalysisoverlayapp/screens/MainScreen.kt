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

package com.arcgismaps.toolkit.sceneviewanalysisoverlayapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.analysis.LocationViewshed
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.toolkit.geocompose.SceneView

/**
 * Displays a composable [SceneView] with an [AnalysisOverlay] and a [GraphicsOverlay].
 * The [AnalysisOverlay] is used to display a [LocationViewshed] and the [GraphicsOverlay]
 * is used to display a [Graphic] at the location for the observer of the [LocationViewshed]. Using
 * the [Switch] in the [TopAppBar] the [AnalysisOverlay] can be toggled on and off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember {
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            val surface = Surface()
            surface.elevationSources.add(
                ArcGISTiledElevationSource(
                    "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
                )
            )
            baseSurface = surface

            val buildingsSceneLayer =
                ArcGISSceneLayer("https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Brest/SceneServer/layers/0")
            operationalLayers.add(buildingsSceneLayer)
        }
    }

    var checked by remember { mutableStateOf(true) }

    val initLocation = remember { Point(-4.50, 48.4, 1000.0) }
    val cameraController = remember { OrbitLocationCameraController(initLocation, 5000.0) }

    val viewshed = remember {
        LocationViewshed(
            initLocation,
            82.0,
            60.0,
            120.0,
            120.0,
            0.0,
            1500.0
        )
    }
    val analysisOverlay = remember { AnalysisOverlay(listOf(viewshed)).apply { isVisible = true } }
    val analysisOverlays = remember { listOf(analysisOverlay) }

    val graphicsOverlay = GraphicsOverlay(
        listOf(
            Graphic(initLocation).apply {
                val viewpointSymbol = SimpleMarkerSymbol(
                    SimpleMarkerSymbolStyle.Diamond,
                    color = Color.cyan,
                    size = 32f
                )
                symbol = viewpointSymbol
            }
        )
    )
    val graphicsOverlays = remember { listOf(graphicsOverlay) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("SceneView Analysis Overlay App")
                },
                actions = {
                    Column {
                        Switch(
                            checked,
                            onCheckedChange = {
                                analysisOverlay.isVisible = it
                                checked = it
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SceneView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            arcGISScene = arcGISScene,
            cameraController = cameraController,
            graphicsOverlays = graphicsOverlays,
            analysisOverlays = analysisOverlays
        )
    }
}
