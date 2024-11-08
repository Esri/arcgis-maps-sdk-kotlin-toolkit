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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.SceneViewInteractionOptions
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewProxy
import com.arcgismaps.toolkit.ar.TableTopSceneViewStatus
import com.arcgismaps.toolkit.ar.rememberTableTopSceneViewStatus
import com.arcgismaps.toolkit.artabletopapp.R
import kotlin.math.roundToInt

@Composable
fun MainScreen() {
    val arcGISScene = remember {
        ArcGISScene().apply {
            operationalLayers.add(
                ArcGISSceneLayer("https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/DevA_BuildingShells/SceneServer")
            )
            baseSurface = Surface().apply {
                elevationSources.add(
                    ElevationSource.fromTerrain3dService()
                )
                opacity = 0f
            }
        }
    }
    // disable pan/zoom/rotate interaction. These interactions can behave unexpectedly in TableTop scenarios
    val interactionOptions = remember {
        SceneViewInteractionOptions().apply {
            this.isPanEnabled = false
            this.isZoomEnabled = false
            this.isRotateEnabled = false
            this.isFlingEnabled = false
        }
    }
    val tableTopSceneViewProxy = remember { TableTopSceneViewProxy() }
    var tappedLocation by remember { mutableStateOf<Point?>(null) }
    var initializationStatus: TableTopSceneViewStatus by rememberTableTopSceneViewStatus()
    val arcGISSceneAnchor = remember {
        Point(-122.68350326165559, 45.53257485106716, 0.0, arcGISScene.spatialReference)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        TableTopSceneView(
            arcGISScene = arcGISScene,
            arcGISSceneAnchor = arcGISSceneAnchor,
            translationFactor = 400.0,
            modifier = Modifier.fillMaxSize(),
            clippingDistance = 400.0,
            tableTopSceneViewProxy = tableTopSceneViewProxy,
            sceneViewInteractionOptions = interactionOptions,
            onInitializationStatusChanged = {
                initializationStatus = it
            },
            onSingleTapConfirmed = {
                val location = tableTopSceneViewProxy.screenToBaseSurface(it.screenCoordinate)
                location?.let { point ->
                    tappedLocation = point
                }
            }
        ) {
            tappedLocation?.let {
                Callout(location = it, modifier = Modifier.wrapContentSize()) {
                    Text(stringResource(R.string.lat_lon, it.y.roundToInt(), it.x.roundToInt()))
                }
            }
        }
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