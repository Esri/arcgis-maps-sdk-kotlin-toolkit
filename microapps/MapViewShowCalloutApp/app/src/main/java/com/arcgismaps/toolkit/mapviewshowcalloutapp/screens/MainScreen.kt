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

package com.arcgismaps.toolkit.mapviewshowcalloutapp.screens

import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
//import com.arcgismaps.mapping.view.Callout
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geocompose.Callout
import com.arcgismaps.toolkit.geocompose.CalloutPlacementOperation
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation

/**
 *
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val disneyLand = remember { Point(-117.9190, 33.8121, SpatialReference.wgs84()) }
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISImagery)) }
    var calloutPlacementOperation: CalloutPlacementOperation? by remember { mutableStateOf(null) }
//    var callout: Callout? by remember { mutableStateOf(null) }
    var callout: @Composable (() -> Unit)? by remember {
        mutableStateOf(
            null
//            { Callout(location = disneyLand) { Text("Hello, World!", color = Color.Green) } }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("MapView Show Callout App") },
                actions = {
                    var actionsExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    ShowCalloutDropdownMenu(
                        expanded = actionsExpanded,
//                        onSetCalloutPlacementOperation = { calloutPlacementOperation = it },
                        onSetCallout = { callout = it },
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
                modifier = Modifier.fillMaxSize(),
                arcGISMap = arcGISMap,
                mapViewProxy = MapViewProxy(),
                calloutPlacementOperation = calloutPlacementOperation,
//                callout = callout
//                callout = Callout(location = disneyLand) {
//                    Text("Hello, World!", color = Color.Green)
//                }
                content = callout
            )
//            {
//               Callout(it, location = disneyLand) { Text("Hello, World!", color = Color.Green) }
        }
    }
}


/**
 * A drop down menu providing [SceneViewpointOperation]s for the composable [SceneView].
 */
@Composable
fun ShowCalloutDropdownMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
//    onSetCalloutPlacementOperation: (CalloutPlacementOperation) -> Unit,
    onSetCallout: (@Composable ()-> Unit) -> Unit,
    onDismissRequest: () -> Unit
) {
    val calloutOperationsList = remember {
        listOf("Show Callout Location", "Show Callout Geoelement", "Dismiss Callout")
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
        calloutOperationsList.forEach {
            val calloutOperationName = it
            val context = LocalContext.current
            val text = TextView(context).apply {
                text = "lorem ipsum"
            }
            DropdownMenuItem(
                text = { Text(text = calloutOperationName) },
                onClick = {
                    val calloutPlacementOperation = when (calloutOperationName) {
//                        "Show Callout Location" -> CalloutPlacementOperation.ShowTextAtLocation(context, "lorem ipsum", disneyLand)
                        "Show Callout Location" -> onSetCallout {
                            Callout(location = disneyLand) {
                                Text(
                                    "Hello, World!",
                                    color = Color.Green
                                )
                            }
                        }
//                        "Dismiss Callout" -> CalloutPlacementOperation.Dismiss()
                        "Dismiss Callout" -> onSetCallout{ null }

                        else -> error("Unexpected MapViewpointOperation")
                    }
//                    onSetCalloutPlacementOperation(calloutPlacementOperation)
                    onDismissRequest()
                }
            )
        }
    }
}

