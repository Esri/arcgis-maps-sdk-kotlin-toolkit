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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.arcgismaps.toolkit.geocompose.SceneViewProxy
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Displays a composable [SceneView] and permits setting the viewpoint using options in a dropdown menu.
 * The dropdown menu options represent different methods of setting the viewpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene by remember { mutableStateOf(ArcGISScene(BasemapStyle.ArcGISImagery)) }
    val sceneViewProxy = remember { SceneViewProxy() }
    val coroutineScope = rememberCoroutineScope()

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
                        onSelectMethod = {
                            coroutineScope.launch {
                                it.method.invoke(sceneViewProxy)
                            }
                        },
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
                sceneViewProxy = sceneViewProxy
            )
        }
    }
}

/**
 * A drop down menu providing methods of setting the viewpoint for the composable [SceneView].
 */
@Composable
fun SetViewpointDropdownMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onSelectMethod: (SetViewpointMethod) -> Unit,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        SetViewpointMethod.entries.forEach { setViewpointMethod ->
            DropdownMenuItem(
                text = { Text(text = setViewpointMethod.label) },
                onClick = { onSelectMethod(setViewpointMethod) }
            )
        }
    }
}

/**
 * An enum class representing different methods of setting the viewpoint on a composable [SceneView].
 *
 * Each entry has a [label] for displaying in a drop down along with a [method] property that calls
 * the matching method on the passed-in [SceneViewProxy].
 */
enum class SetViewpointMethod(val label: String, val method: suspend (SceneViewProxy) -> Unit) {
    SET_VIEWPOINT(
        "setViewpoint",
        { sceneViewProxy ->
            sceneViewProxy.setViewpoint(
                // Disneyland
                Viewpoint(
                    Point(
                        -117.9190,
                        33.8121,
                        SpatialReference.wgs84()
                    ), 170000.0
                )
            )
        }
    ),

    SET_VIEWPOINT_ANIMATED(
        "setViewpointAnimated",
        { sceneViewProxy ->
            sceneViewProxy.setViewpointAnimated(
                // Sofia, Bulgaria
                Viewpoint(Point(23.321736, 42.697703, SpatialReference.wgs84()), 170000.0),
                5.0.seconds
            )
        }
    ),

    SET_VIEWPOINT_CAMERA(
        "setViewpointCamera",
        { sceneViewProxy ->
            sceneViewProxy.setViewpointCamera(
                // Catalina
                Camera(
                    Point(
                        -118.61832205396796,
                        33.48526535148485,
                        803.4239338943735,
                        SpatialReference.wgs84()
                    ), 122.5, 77.0, 0.0
                )
            )
        }
    ),

    SET_VIEWPOINT_CAMERA_ANIMATED(
        "setViewpointCameraAnimated",
        { sceneViewProxy ->
            sceneViewProxy.setViewpointCameraAnimated(
                // Golden Gate Bridge
                Camera(
                    Point(
                        -122.529736915401,
                        37.83466841571218,
                        1485.2682057125494,
                        SpatialReference.wgs84()
                    ), 111.3, 71.7, 0.0
                ),
                5.0.seconds
            )
        }
    ),

    SET_BOOKMARK(
        "setBookmark",
        { sceneViewProxy ->
            sceneViewProxy.setBookmark(
                Bookmark(
                    "Rotterdam",
                    Viewpoint(Point(4.4777, 51.9244, SpatialReference.wgs84()), 170000.0)
                )
            )
        }
    )
}