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

package com.arcgismaps.toolkit.mapcomposeapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.MapState
import com.arcgismaps.toolkit.mapcomposeapp.ui.theme.MapComposeAppTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    // Preview window compose code here:
    if (isPreview()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display preview Map
            Map(modifier = Modifier.fillMaxSize().weight(1f))

            // Set preview content
            SetViewpointButton(modifier = Modifier.wrapContentSize())
        }

    }
    // Non preview code here:
    else {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // set up a viewpoint
            val londonPoint = Point(-14093.0, 6711377.0, SpatialReference.webMercator())
            val londonViewpoint = Viewpoint(londonPoint, 10000000.0)

            // set the ArcGISMap to display the London viewpoint
            val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
                initialViewpoint = londonViewpoint
            }

            // create the MapState
            val mapState = remember { MapState(arcGISMap = arcGISMap) }

            // add the Compose Map using the MapState to show map with London viewpoint
            Map(modifier = Modifier.fillMaxSize().weight(1f,fill = false),mapState = mapState)
        }
    }
}

@Composable
fun SetViewpointButton(modifier: Modifier = Modifier, mapState: MapState = MapState()) {
    val scope = rememberCoroutineScope()

    // Preview code here:
    if (isPreview()) {
        Button(modifier = modifier, onClick = { }) { Text(text = "Set new viewpoint") }
        return
    }

    // Non preview code here:
    Button(
        modifier = modifier,
        onClick = {
            scope.launch {
                val londonPoint = Point(-14093.0, 6711377.0, SpatialReference.webMercator())
                val newScale = 1000000.0
                // animated set viewpoint functions
                mapState.setViewpointCenter(londonPoint, newScale).onSuccess {
                    // Get the current viewpoint after center animation
                    val viewpoint = mapState.getCurrentViewpoint(ViewpointType.CenterAndScale)
                }
            }
        }) {
        Text(text = "Set new viewpoint")
    }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
internal fun MainScreenPreview() {
    MapComposeAppTheme {
        MainScreen(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun isPreview(): Boolean = LocalInspectionMode.current