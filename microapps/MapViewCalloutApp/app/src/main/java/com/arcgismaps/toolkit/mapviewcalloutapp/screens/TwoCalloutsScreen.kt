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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun ShowTwoCalloutsScreen(viewModel: MapViewModel) {

    var buttonState by remember { mutableStateOf(true) }

    Box {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.arcGISMap,
            mapViewProxy = viewModel.mapViewProxy,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            insets = PaddingValues(horizontal = 30.dp),
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                viewModel.identify(singleTapConfirmedEvent)
            },
            content =
            {
                if (buttonState) {
                    Callout(
                        location = Point(-122.0 , 49.0, SpatialReference.wgs84()),
                        modifier = Modifier.wrapContentSize()
                        //tapLocation = viewModel.tapLocation.value,
                    ) {
                        Column {
                            Text(
                                text = "Callout 1",
                                style = MaterialTheme.typography.labelSmall
                            )

                        }
                    }
                } else {
                    Callout(
                        location = Point(-124.0 , 49.0, SpatialReference.wgs84()),
                        modifier = Modifier.wrapContentSize()
                        //tapLocation = viewModel.tapLocation.value,
                    ) {
                        Column {
                            Text(
                                text = "Callout 2",
                                style = MaterialTheme.typography.labelSmall
                            )

                        }
                    }
                }
            }
        )
        Button(onClick = { buttonState = !buttonState }) {
            Text("Toggle Callout")
        }
    }
}