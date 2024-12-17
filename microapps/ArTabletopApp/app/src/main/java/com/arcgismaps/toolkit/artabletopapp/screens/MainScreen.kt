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

package com.arcgismaps.toolkit.artabletopapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewProxy
import com.arcgismaps.toolkit.ar.TableTopSceneViewStatus
import com.arcgismaps.toolkit.ar.rememberTableTopSceneViewStatus
import com.arcgismaps.toolkit.artabletopapp.R
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val arcGISSceneLayer = remember {
        ArcGISSceneLayer("https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/DevA_BuildingShells/SceneServer")
    }
    val sr = arcGISSceneLayer.spatialReference
    val arcGISSceneAnchor = remember {
        Point(-122.68350326165559, 45.53257485106716, 10.0, sr)
    }
    val arcGISScene = remember {
        ArcGISScene().apply {
//            operationalLayers.add(arcGISSceneLayer)
            baseSurface = Surface().apply {
                elevationSources.add(
                    ElevationSource.fromTerrain3dService()
                )
//                opacity = 0f
            }
            initialViewpoint = Viewpoint(arcGISSceneAnchor, 1000.0)
        }
    }

    // Tracks the currently selected building
    var identifiedBuilding by remember { mutableStateOf<IdentifiedBuilding?>(null) }

    var initializationStatus: TableTopSceneViewStatus by rememberTableTopSceneViewStatus()
    val tableTopSceneViewProxy = remember { TableTopSceneViewProxy() }
    val coroutineScope = rememberCoroutineScope()
    val graphicsOverlays = remember { listOf(GraphicsOverlay()) }
    LaunchedEffect(Unit) {
        arcGISScene.loadStatus.filter { it.isTerminal }.collect {
            graphicsOverlays.first().graphics.addAll(listOf(
                Graphic(
                    arcGISSceneAnchor,
                    SimpleMarkerSceneSymbol(SimpleMarkerSceneSymbolStyle.Sphere, com.arcgismaps.Color.red, height = 10.0, width = 10.0, depth = 10.0)
                )
            ))
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        TableTopSceneView(
            arcGISScene = arcGISScene,
            arcGISSceneAnchor = arcGISSceneAnchor,
            translationFactor = 2000.0,
            modifier = Modifier.fillMaxSize(),
            clippingDistance = 750.0,
            tableTopSceneViewProxy = tableTopSceneViewProxy,
            onInitializationStatusChanged = {
                initializationStatus = it
            },
            onSingleTapConfirmed = { tap ->
                arcGISSceneLayer.clearSelection()
                coroutineScope.launch {
                    identifiedBuilding = arcGISSceneLayer.identifyBuilding(
                        tap.screenCoordinate,
                        tableTopSceneViewProxy
                    )
                    identifiedBuilding?.let { identifiedBuilding ->
                        arcGISSceneLayer.selectFeature(identifiedBuilding.feature)
                    }
                }

                graphicsOverlays.first().graphics.add(
                    Graphic(
                        tableTopSceneViewProxy.screenToBaseSurface(tap.screenCoordinate) ?: return@TableTopSceneView,
                        SimpleMarkerSceneSymbol(SimpleMarkerSceneSymbolStyle.Cube, com.arcgismaps.Color.green, 10.0, 10.0, 10.0)
                    )
                )
            },
            graphicsOverlays = graphicsOverlays
        ) {
            identifiedBuilding?.let {
                Callout(it.location) {
                    Text("Building ID: ${it.feature.attributes["OBJECTID"]}")
                }
            }
        }
    }

    // Show an overlay with instructions or progress indicator based on the initialization status
    when (val status = initializationStatus) {
        is TableTopSceneViewStatus.Initializing -> TextWithScrim(text = stringResource(R.string.initializing_overlay))
        is TableTopSceneViewStatus.DetectingPlanes -> TextWithScrim(text = stringResource(R.string.detect_planes_overlay))
        is TableTopSceneViewStatus.Initialized -> {
            val sceneLoadStatus = arcGISScene.loadStatus.collectAsStateWithLifecycle().value
            when (sceneLoadStatus) {
                is LoadStatus.NotLoaded -> {
                    // Tell the user to tap the screen if the scene has not started loading
                    TextWithScrim(text = stringResource(R.string.tap_scene_overlay))
                }

                is LoadStatus.Loading -> {
                    // The scene may take a while to load, so show a progress indicator
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LoadStatus.FailedToLoad -> {
                    TextWithScrim(
                        text = stringResource(
                            R.string.failed_to_load_scene,
                            sceneLoadStatus.error
                        )
                    )
                }

                LoadStatus.Loaded -> {} // Do nothing
            }
        }

        is TableTopSceneViewStatus.FailedToInitialize -> {
            TextWithScrim(
                text = stringResource(
                    R.string.failed_to_initialize_overlay,
                    status.error.message ?: status.error
                )
            )
        }
    }
}

/**
 * Displays the provided [text] on top of a half-transparent gray background.
 *
 * @since 200.6.0
 */
@Composable
fun TextWithScrim(text: String) {
    Column(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.5f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

/**
 * Identifies the building at the given [screenCoordinate] and returns the identified building.
 * If no feature is identified, or if no location can be found for the given [screenCoordinate],
 * this function returns `null`.
 *
 * @since 200.6.0
 */
private suspend fun ArcGISSceneLayer.identifyBuilding(
    screenCoordinate: ScreenCoordinate,
    proxy: TableTopSceneViewProxy
): IdentifiedBuilding? {
    val identifyLayerResult = proxy.identify(this, screenCoordinate, 50.dp).getOrElse {
        return null
    }
    val identifiedFeature =
        identifyLayerResult.geoElements.firstOrNull() as? ArcGISFeature ?: return null
    val identifiedPoint = proxy.screenToLocation(screenCoordinate).getOrNull() ?: return null
    return IdentifiedBuilding(identifiedFeature, identifiedPoint)
}

/**
 * Represents a building feature along with the location in the scene where it was identified.
 *
 * @since 200.6.0
 */
private data class IdentifiedBuilding(val feature: ArcGISFeature, val location: Point)

val ArcGISScene.center: Point
    get() {
        var envelope = Envelope(0.0, 0.0, 0.0, 0.0, spatialReference = this.spatialReference)
        operationalLayers.forEach { layer ->
            envelope =
                layer.fullExtent?.let { it -> GeometryEngine.union(envelope, it) }?.extent ?: envelope
        }
        return envelope.center
    }