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
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }
    var mapViewpointOperation: MapViewpointOperation? by remember {
        mutableStateOf(null)
    }
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
                        onSetViewpointOperation = {
                            mapViewpointOperation = it
                        },
                        expanded = actionsExpanded
                    ) {
                        actionsExpanded = false
                    }
                }
            )
        },
    ) { innerPadding ->
        MapView(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            arcGISMap = arcGISMap,
            viewpointOperation = mapViewpointOperation
        )
    }
}

@Composable
fun SetViewpointDropdownMenu(
    onSetViewpointOperation: (MapViewpointOperation) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val items = remember {
        MapViewpointOperation::class.sealedSubclasses
    }
    val sofia = remember {
        Point(23.321736, 42.697703, SpatialReference.wgs84())
    }
    val scale = remember { 170000.0 }
    val sofiaBounds = remember {
        GeometryEngine.bufferOrNull(sofia, 0.083) ?: error("Couldn't buffer point")
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        items.forEach {
            val name = it.simpleName ?: error("No name found for operation type")
            DropdownMenuItem(
                text = { Text(text = name) },
                onClick = {
                    val viewpointOperation = when (name) {
                        "Animate" -> MapViewpointOperation.Animate(
                                    Viewpoint(sofia, scale),
                                    5f,
                                    AnimationCurve.EaseOutCubic
                        )
                        "Center" -> MapViewpointOperation.Center(sofia, scale)
                        "Rotate" -> MapViewpointOperation.Rotate(0.0)
                        "Scale" -> MapViewpointOperation.Scale(scale)
                        "Set" -> MapViewpointOperation.Set(Viewpoint(sofia, scale))
                        "SetBookmark" -> MapViewpointOperation.SetBookmark(
                            Bookmark(
                                "Sofia",
                                Viewpoint(sofia, scale)
                            )
                        )
                        "SetBoundingGeometry" -> MapViewpointOperation.SetBoundingGeometry(sofiaBounds)
                        else -> error("Unexpected MapViewpointOperation")
                    }
                    onSetViewpointOperation(viewpointOperation)
                    onDismissRequest()
                }
            )
        }
    }
}
