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
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.CameraController
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.mapping.view.GlobeCameraController
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.OrbitGeoElementCameraController
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.toolkit.geocompose.SceneViewProxy
import kotlin.time.Duration.Companion.seconds

/**
 * Display a composable [SceneView] and demonstrate how to use different [CameraController]s to
 * navigate viewpoints in the scene view.
 *
 * There are three menu actions in the dropdown menu:
 * - Global: Use [GlobeCameraController] to navigate the scene view.
 * - Orbit (GeoElement): Use an [OrbitGeoElementCameraController] to navigate the scene view to the desired [GeoElement].
 * - Orbit (Location): Use an [OrbitLocationCameraController] to navigate the scene view to the desired location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        val surfaceUrl = "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        val elevationSource = ArcGISTiledElevationSource(surfaceUrl)
        this.baseSurface.elevationSources.add(elevationSource)
    } }

    // a red 3D Sphere at the center of a crater
    val simpleGraphic = remember {
        Graphic(
            Point(-109.929589, 38.437304, 5000.0, SpatialReference.wgs84()),
            SimpleMarkerSceneSymbol(style = SimpleMarkerSceneSymbolStyle.Sphere, color = Color.red)
        )
    }
    // a list of GraphicsOverlays used by the SceneView
    val graphicsOverlays = remember {
        listOf(
            GraphicsOverlay().apply {
                graphics.add(simpleGraphic)
            }
        )
    }

    // remember a CameraController state
    var cameraController: CameraController by remember { mutableStateOf(GlobeCameraController()) }

    // remember a SceneViewProxy
    val sceneViewProxy = remember { SceneViewProxy() }

    Scaffold(
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
                        targetGeoElement = simpleGraphic
                    )
                }
            )
        }
    ) { paddingValues ->

        SceneView(
            arcGISScene,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraController = cameraController,
            graphicsOverlays = graphicsOverlays,
            sceneViewProxy = sceneViewProxy
        )

        LaunchedEffect(cameraController) {
            (cameraController as? OrbitLocationCameraController)?.moveCamera(
                distanceDelta = 8000.0,
                headingDelta = 5.0,
                pitchDelta = 5.0,
                duration = 6f
            ) ?: (cameraController as? OrbitGeoElementCameraController)?.moveCamera(
                pitchDelta = 30.0,
                distanceDelta = 6000.0,
                headingDelta = 150.0,
                duration = 10f
            ) ?: (cameraController as? GlobeCameraController)?.apply {
                sceneViewProxy.setViewpointCameraAnimated(
                    Camera(
                        latitude = 34.05,
                        longitude = -117.19,
                        altitude = 10000000.0,
                        heading = 0.0,
                        pitch = 0.0,
                        roll = 0.0
                    ),
                    duration = 5.0.seconds
                )
            }
        }
    }
}

/**
 * Composable function that displays a dropdown menu for selecting different camera controllers
 * to manipulate a SceneView.
 *
 * @param expanded Whether the dropdown menu is expanded or not
 * @param modifier Modifier for styling and positioning the dropdown menu
 * @param targetGeoElement The target graphic element for camera control
 * @param onCameraControllerChanged Callback triggered when the camera controller is changed
 * @param onDismissRequest Callback triggered when the dropdown menu is dismissed200
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
