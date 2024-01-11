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

package com.arcgismaps.toolkit.sceneviewcameracontrollerapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.CameraController
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.mapping.view.GlobeCameraController
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.OrbitGeoElementCameraController
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation
import com.arcgismaps.toolkit.geocompose.rememberGraphicsOverlayCollection
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val sceneViewModel = remember { SceneViewModel() }
    var cameraController: CameraController by remember { mutableStateOf(GlobeCameraController()) }
    var viewpointOperation: SceneViewpointOperation? by remember { mutableStateOf(null) }

    Scaffold (
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { Text("SceneView Camera Controller") },
                actions = {
                    var actionsExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    SetCameraControllerDropdownMenu(
                        expanded = actionsExpanded,
                        onCameraControllerChanged = { cameraController = it },
                        onDismissRequest = { actionsExpanded = false },
                        targetGeoElement = sceneViewModel.simpleGraphic
                    )
                }
            )
        }
    ) { paddingValues ->

        SceneView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            arcGISScene = sceneViewModel.arcGISScene,
            cameraController = cameraController,
            viewpointOperation = viewpointOperation,
            graphicsOverlays = rememberGraphicsOverlayCollection {
                add(sceneViewModel.sceneGraphicsOverlay)
            }
        )

        LaunchedEffect(cameraController) {
            (cameraController as? OrbitLocationCameraController)?.moveCamera(
                distanceDelta = 8000.0,
                headingDelta = 5.0,
                pitchDelta = 5.0,
                duration = 6f
            )
            ?: (cameraController as? OrbitGeoElementCameraController)?.moveCamera(
                pitchDelta = 30.0,
                distanceDelta = 6000.0,
                headingDelta = 150.0,
                duration = 10f
            )
            ?: (cameraController as? GlobeCameraController)?.apply {
                viewpointOperation = SceneViewpointOperation.AnimateCamera(
                    Camera(34.05, -117.19, 10000000.0,0.0,0.0,0.0),
                    duration = 5.0.seconds
                    )
            }
        }
    }
}

/**
 * Set camera controller dropdown menu
 *
 * @param expanded
 * @param modifier
 * @param onCameraControllerChanged
 * @param onDismissRequest
 * @param targetGeoElement
 * @receiver
 * @receiver
 */
@Composable
fun SetCameraControllerDropdownMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    targetGeoElement: Graphic,
    onCameraControllerChanged: (CameraController) -> Unit,
    onDismissRequest: () -> Unit
) {
    val cameraControllerList = remember {
        listOf("Global", "Orbit(GeoElement)", "Orbit(Location)")
    }

    val esriRedlands = remember {
        Point(-13046169.058474509,  4036520.731959608, SpatialReference.webMercator())
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        cameraControllerList.forEach { name ->
            DropdownMenuItem(
                text = { Text(text = name) },
                onClick = {
                    val cameraController = when (name) {
                        "Global" -> {
                            GlobeCameraController()
                        }
                        "Orbit(GeoElement)" -> {
                            OrbitGeoElementCameraController(targetGeoElement, 600.0)
                        }
                        "Orbit(Location)" -> {
                            OrbitLocationCameraController(esriRedlands, 400.0)
                        }

                        else -> error("Unsupported actions")
                    }
                    onCameraControllerChanged(cameraController)
                    onDismissRequest()
                }
            )
        }
    }
}
