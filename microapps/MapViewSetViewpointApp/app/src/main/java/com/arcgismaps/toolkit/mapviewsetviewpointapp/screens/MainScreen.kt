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
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Displays a composable [MapView] and permits setting the viewpoint using options in a dropdown menu.
 * The dropdown menu options represent different methods of setting the viewpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }
    val mapViewProxy = remember { MapViewProxy() }
    var showProgressIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
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
                        onSelectViewpointMethod = {
                            actionsExpanded = false
                            coroutineScope.launch {
                                showProgressIndicator = true
                                it.method(mapViewProxy)
                                showProgressIndicator = false
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
            MapView(
                arcGISMap,
                modifier = Modifier.fillMaxSize(),
                mapViewProxy = mapViewProxy
            )
            if (showProgressIndicator) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * A drop down menu providing methods of setting the viewpoint for the composable [MapView].
 */
@Composable
fun SetViewpointDropdownMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onSelectViewpointMethod: (SetViewpointMethod) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        SetViewpointMethod.entries.forEach {
            DropdownMenuItem(
                text = { Text(text = it.label) },
                onClick = { onSelectViewpointMethod(it) }
            )
        }
    }
}

/**
 * An enum class representing different methods of setting the viewpoint on a composable [MapView].
 *
 * Each entry has a [label] for displaying in a drop down along with a [method] property that calls
 * the matching method on the passed-in [MapViewProxy].
 */
enum class SetViewpointMethod(val label: String, val method: suspend (MapViewProxy) -> Unit) {
    SET_VIEWPOINT(
        "setViewpoint",
        { mapViewProxy ->
            mapViewProxy.setViewpoint(
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
        { mapViewProxy ->
            mapViewProxy.setViewpointAnimated(
                // Sofia, Bulgaria
                Viewpoint(
                    Point(
                        23.321736,
                        42.697703,
                        SpatialReference.wgs84()
                    ), 170000.0
                ),
                5.0.seconds,
                AnimationCurve.EaseOutCubic
            )
        }
    ),

    SET_VIEWPOINT_CENTER(
        "setViewpointCenter",
        { mapViewProxy ->
            mapViewProxy.setViewpointCenter(
                // Catalina
                Point(
                    -118.419710,
                    33.432245,
                    SpatialReference.wgs84()
                ),
                1000000.0
            )
        }
    ),

    SET_VIEWPOINT_ROTATION(
        "setViewpointRotation (to North)",
        { mapViewProxy ->
            mapViewProxy.setViewpointRotation(0.0)
        }
    ),

    SET_VIEWPOINT_SCALE(
        "setViewpointScale (to 170,000)",
        { mapViewProxy ->
            mapViewProxy.setViewpointScale(170000.0)
        }
    ),

    SET_VIEWPOINT_GEOMETRY(
        "setViewpointGeometry",
        { mapViewProxy ->
            // Golden gate bridge
            mapViewProxy.setViewpointGeometry(
                Envelope(
                    -122.612736915401,
                    37.75166841571218,
                    -122.446736915401,
                    37.91766841571218,
                    spatialReference = SpatialReference.wgs84()
                )
            )
        }
    ),

    SET_BOOKMARK(
        "setBookmark",
        { mapViewProxy ->
            mapViewProxy.setBookmark(
                com.arcgismaps.mapping.Bookmark(
                    "Rotterdam",
                    Viewpoint(Point(4.4777, 51.9244, SpatialReference.wgs84()), 170000.0)
                )
            )
        }
    )
}
