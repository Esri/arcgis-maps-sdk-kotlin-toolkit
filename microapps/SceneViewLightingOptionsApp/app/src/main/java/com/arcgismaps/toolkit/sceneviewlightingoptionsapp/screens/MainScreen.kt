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

package com.arcgismaps.toolkit.sceneviewlightingoptionsapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.toolkit.geocompose.SceneViewProxy
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISImagery) }
    val camera = Camera(Point(-73.0815, -49.3272, 4059.0, SpatialReference.wgs84()), 11.0, 62.0, 0.0)
// start up a long animation so we can test renderFrame
    var viewpointOperation by remember { mutableStateOf<SceneViewpointOperation?>(SceneViewpointOperation.AnimateCamera(camera, 120.0.seconds)) }
    val sceneViewProxy = remember {
        SceneViewProxy()
    }
    var checked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            var actionsExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("SceneView Lighting Options App")
                },
                actions = {
                    Column {
                        Switch(
                            checked,
                            onCheckedChange = {
                                checked = it
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                        }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
    ) { innerPadding ->
        SceneView(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            arcGISScene = arcGISScene,
            viewpointOperation = viewpointOperation,
            sceneViewProxy = sceneViewProxy,
        )
    }
}
