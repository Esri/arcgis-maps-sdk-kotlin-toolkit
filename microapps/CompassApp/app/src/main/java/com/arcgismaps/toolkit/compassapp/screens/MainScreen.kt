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

package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation

@Composable
fun MainScreen() {
    // create an ArcGISMap with a Topographic basemap style
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISTopographic)) }
    // set the composable map's viewpoint to North America
    var mapViewpointOperation: MapViewpointOperation? by remember {
        mutableStateOf(
            MapViewpointOperation.Set(Viewpoint(39.8, -98.6, 10e7))
        )
    }
    var mapRotation by remember { mutableDoubleStateOf(0.0) }
    // show composable MapView with compass
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
            viewpointOperation = mapViewpointOperation,
            onMapRotationChanged = { rotation -> mapRotation = rotation }
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
                .padding(25.dp)
        ) {
            // show the compass and pass the mapRotation state data
            Compass(rotation = mapRotation) {
                // reset the Composable MapView viewpoint rotation to point north
                mapViewpointOperation = MapViewpointOperation.Rotate(0.0)
            }
        }
    }
}
