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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun DynamicEntityScreen(viewModel: MapViewModel) {
    Box {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.mapWithDynamicEntities,
            mapViewProxy = viewModel.mapViewProxy,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            insets = PaddingValues(horizontal = 30.dp),
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                viewModel.identifyOnDynamicEntity(singleTapConfirmedEvent)
            },
            content =
            {

                val selectedGeoElement = viewModel.selectedGeoElement.collectAsState().value
                    ?: return@MapView
                Callout(
                    geoElement = selectedGeoElement,
                    modifier = Modifier.wrapContentSize(),
                ) {
                    key(viewModel.dynamicEntityObservationId.collectAsState().value) {
                        Column {
                            Text(
                                text = "${selectedGeoElement.attributes}",
                                style = MaterialTheme.typography.labelSmall
                            )

                        }
                    }
                }
            }

        )
    }
}