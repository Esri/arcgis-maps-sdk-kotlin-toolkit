/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.arrealitycaptureapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.ar.WorldScaleTrackingMode
import com.arcgismaps.toolkit.ar.rememberWorldScaleSceneViewStatus
import com.arcgismaps.toolkit.arrealitycaptureapp.R

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel()
) {
    val arcGISScene = remember {
        val basemap = Basemap(BasemapStyle.ArcGISHumanGeography)
        ArcGISScene(basemap).apply {
            // an elevation source is required for the scene to be placed at the correct elevation
            // if not used, the scene may appear far below the device position because the device position
            // is calculated with elevation
            baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
            baseSurface.backgroundGrid.isVisible = false
            baseSurface.opacity = 0.5f
            // add the Esri 3D Buildings layer
            operationalLayers.add(
                ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=b8fec5af7dfe4866b1b8ac2d2800f282").apply {
                    this.opacity = 0.5f
                }
            )
        }
    }
    var initializationStatus by rememberWorldScaleSceneViewStatus()
    var trackingError by remember { mutableStateOf<Throwable?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Upper row: WorldScaleSceneView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            WorldScaleSceneView(
                arcGISScene = arcGISScene,
                modifier = Modifier
                    .fillMaxSize(),
                worldScaleTrackingMode = WorldScaleTrackingMode.Geospatial(),
                clippingDistance = 100.0,
                onInitializationStatusChanged = {
                    initializationStatus = it
                },
                worldScaleSceneViewProxy = viewModel.proxy,
                onTrackingErrorChanged = {
                    trackingError = it
                },
                onSingleTapConfirmed = { singleTapConfirmedEvent -> },
                graphicsOverlays = viewModel.graphicsOverlays
            )
        }

        // Lower row - initialization status and capture button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when(val status = initializationStatus) {
                is WorldScaleSceneViewStatus.Initializing -> {
                    Text(
                        text = "Initializing VPS...",
                        modifier = Modifier.padding(32.dp)
                    )
                }
                is WorldScaleSceneViewStatus.Initialized -> {
                    if (trackingError == null) {
                        FloatingActionButton(
                            modifier = Modifier.padding(32.dp),
                            onClick = {
                                viewModel.captureOrientedImage()
                            },
                            shape = CircleShape
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_photo_camera_24px),
                                contentDescription = "Take photo"
                            )
                        }
                    } else {
                        Text(
                            text = "Tracking error: ${trackingError?.message}",
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                is WorldScaleSceneViewStatus.FailedToInitialize -> {
                    Text(
                        text = "Failed to initialize VPS: ${status.error.message}",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}