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

package com.arcgismaps.toolkit.sceneviewsetviewpointapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation
import kotlin.time.Duration.Companion.seconds

/**
 * Displays a composable [SceneView] and permits setting the viewpoint using options in a dropdown menu.
 * The dropdown menu options represent different methods of setting the viewpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene by remember { mutableStateOf(ArcGISScene(BasemapStyle.ArcGISImagery)) }
    var sceneViewpointOperation: SceneViewpointOperation? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("SceneView Set Viewpoint App") },
                actions = {
                    var actionsExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    SetViewpointDropdownMenu(
                        expanded = actionsExpanded,
                        onSetViewpointOperation = { sceneViewpointOperation = it },
                        onDismissRequest = { actionsExpanded = false }
                    )
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                arcGISScene = arcGISScene,
                viewpointOperation = sceneViewpointOperation
            )
        }
    }
}

/**
 * A drop down menu providing [SceneViewpointOperation]s for the composable [SceneView].
 */
@Composable
fun SetViewpointDropdownMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onSetViewpointOperation: (SceneViewpointOperation) -> Unit,
    onDismissRequest: () -> Unit
) {
    val viewpointOperationsList = remember {
        listOf("Set", "SetAnimated", "SetCamera", "SetCameraAnimated", "SetBookmark")
    }
    val disneyLand = remember { Point(-117.9190, 33.8121, SpatialReference.wgs84()) }
    val rotterdam = remember { Point(4.4777, 51.9244, SpatialReference.wgs84()) }
    val sofia = remember {
        Point(23.321736, 42.697703, SpatialReference.wgs84())
    }
    val catalina = remember {
        Point(
            -118.61832205396796,
            33.48526535148485,
            803.4239338943735,
            SpatialReference.wgs84()
        )
    }
    val goldenGateBridge = remember {
        Point(
            -122.529736915401,
            37.83466841571218,
            1485.2682057125494,
            SpatialReference.wgs84()
        )
    }
    val catalinaCamera = remember { Camera(catalina, 122.5, 77.0, 0.0) }
    val goldenGateBridgeCamera = remember { Camera(goldenGateBridge, 111.3, 71.7, 0.0) }

    val scale = remember { 170000.0 }
    val bookmark = remember {
        Bookmark(
            "Rotterdam",
            Viewpoint(rotterdam, scale)
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        viewpointOperationsList.forEach {
            val viewpointOperationName = it
            DropdownMenuItem(
                text = { Text(text = viewpointOperationName) },
                onClick = {
                    val viewpointOperation = when (viewpointOperationName) {
                        "Set" -> SceneViewpointOperation.Set(Viewpoint(disneyLand, scale))
                        "SetAnimated" -> SceneViewpointOperation.Animate(
                            Viewpoint(sofia, scale),
                            5.0.seconds
                        )

                        "SetCamera" -> SceneViewpointOperation.SetCamera(catalinaCamera)
                        "SetCameraAnimated" -> SceneViewpointOperation.AnimateCamera(
                            goldenGateBridgeCamera,
                            5.0.seconds
                        )

                        "SetBookmark" -> SceneViewpointOperation.SetBookmark(bookmark)

                        else -> error("Unexpected MapViewpointOperation")
                    }
                    onSetViewpointOperation(viewpointOperation)
                    onDismissRequest()
                }
            )
        }
    }
}
