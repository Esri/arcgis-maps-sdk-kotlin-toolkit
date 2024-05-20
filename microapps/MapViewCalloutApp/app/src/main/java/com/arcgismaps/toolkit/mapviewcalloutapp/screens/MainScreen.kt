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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.Compass
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MapViewModel) {

    val mapViewProxy = remember { MapViewProxy() }
    val coroutineScope = rememberCoroutineScope()

    var mapPoint: Point? by remember { mutableStateOf(null) }
    var mapRotation by remember { mutableDoubleStateOf(0.0) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.arcGISMap,
            mapViewProxy = mapViewProxy,
            onSingleTapConfirmed = { mapPoint = it.mapPoint },
            onMapRotationChanged = { mapRotation = it },
//            compass = {
//                Compass(rotation = mapRotation, autoHide = false) {
//                    // reset the Composable MapView viewpoint rotation to point north
//                    coroutineScope.launch {
//                        mapViewProxy.setViewpointRotation(0.0)
//                    }
//                }
//            },
//            callout = {
//                if (mapPoint != null) {
//                    Callout(location = mapPoint!!) {
//                        Text(
//                            "Hello, World!",
//                            color = Color.Green
//                        )
//                    }
//                }},
            content = {
                Compass(rotation = mapRotation, autoHide = false) {
                    // reset the Composable MapView viewpoint rotation to point north
                    coroutineScope.launch {
                        mapViewProxy.setViewpointRotation(0.0)
                    }
                }
                if (mapPoint != null) {
                    Callout(location = mapPoint!!) {
                        Text(
                            "Hello, World!",
                            color = Color.Green
                        )
                    }
                }
            }
        )
    }
}
