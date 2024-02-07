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

package com.arcgismaps.toolkit.mapviewsetviewpointapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Displays a composable [MapView] and permits setting the viewpoint using options in a dropdown menu.
 * The dropdown menu options represent different methods of setting the viewpoint, and all of them center
 * on the same location. A circular progress indicator is displayed over the map while an operation
 * is in progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }
    val mapViewProxy = remember { MapViewProxy() }
    var showProgressIndicator by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("MapView Set Viewpoint App") },
                actions = {
                    var actionsExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    SetViewpointDropdownMenu(
                        expanded = actionsExpanded,
                        mapViewProxy = mapViewProxy,
                        onDismissRequest = { actionsExpanded = false },
                        onSetLoading = { showProgressIndicator = it }
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
            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = arcGISMap,
                mapViewProxy = mapViewProxy
            )
            if (showProgressIndicator) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * A drop down menu providing methods of setting the viewpoint on a composable [MapView]
 */
@Composable
fun SetViewpointDropdownMenu(
    expanded: Boolean,
    mapViewProxy: MapViewProxy,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onSetLoading: (Boolean) -> Unit
) {
    val viewpointOperationsList = remember {
        listOf("setViewpointAnimated", "setViewpointCenter", "setViewpointRotation", "setViewpointScale", "setViewpoint", "setBookmark", "setViewpointGeometry")
    }
    val sofia = remember {
        Point(23.321736, 42.697703, SpatialReference.wgs84())
    }
    val scale = remember { 170000.0 }
    val bookmark = remember {
        Bookmark(
            "Sofia",
            Viewpoint(sofia, scale)
        )
    }
    val sofiaBounds = remember {
        GeometryEngine.bufferOrNull(sofia, 0.083) ?: error("Couldn't buffer point")
    }
    val coroutineScope = rememberCoroutineScope()
    var setViewpointJob: Job by remember { mutableStateOf(Job()) }
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
                    setViewpointJob.cancel()
                    setViewpointJob = coroutineScope.launch {
                        try {
                            onSetLoading(true)
                            when (viewpointOperationName) {
                                "setViewpointAnimated" -> mapViewProxy.setViewpointAnimated(
                                    Viewpoint(sofia, scale),
                                    5.0.seconds,
                                    AnimationCurve.EaseOutCubic
                                )

                                "setViewpointCenter" -> mapViewProxy.setViewpointCenter(
                                    sofia,
                                    scale
                                )

                                "setViewpointRotation" -> mapViewProxy.setViewpointRotation(0.0)
                                "setViewpointScale" -> mapViewProxy.setViewpointScale(scale)
                                "setViewpoint" -> mapViewProxy.setViewpoint(Viewpoint(sofia, scale))
                                "setBookmark" -> mapViewProxy.setBookmark(bookmark)
                                "setViewpointGeometry" -> mapViewProxy.setViewpointGeometry(
                                    sofiaBounds
                                )

                                else -> error("Unexpected MapViewpointOperation")
                            }
                        } finally {
                            onSetLoading(false)
                        }
                    }
                    onDismissRequest()
                }
            )
        }
    }
}
