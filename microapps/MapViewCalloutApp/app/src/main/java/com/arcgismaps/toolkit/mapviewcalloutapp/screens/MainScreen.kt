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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun MainScreen(viewModel: MapViewModel) {

    val mapPoint = viewModel.mapPoint.collectAsState().value

    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = viewModel.arcGISMap,
        onSingleTapConfirmed = viewModel::setMapPoint,
        content = if (mapPoint != null) {
            {
                Callout(location = mapPoint) {
                    Text(
                        "Hello, World!",
                        color = Color.Green
                    )
                }
                val newPoint = Point(mapPoint.x, mapPoint.y)
                Callout(location = mapPoint, offset = Offset(1.0f, 150.0f)) {
                    Text(
                        "Hello, New World!",
                        color = Color.Red
                    )

                }
            }
        } else {
            null
        }
    )
}
