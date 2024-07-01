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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Point
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlin.math.roundToInt

// TODO Case c.
//- Show a MapView with a map with a GraphicLayer with some graphics
//- Display callout using Point(tap location) on a Graphic (with some HTML content ??)
//- Display a graphic at the tapped location
//- add switch to enable/disable animation

@Composable
fun GraphicScreen(viewModel: MapViewModel){

    val selectedGeoElement = viewModel.selectedGeoElement.collectAsState().value
    var dynamicEntityObservationId by rememberSaveable { mutableLongStateOf(0L) }
    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var nullTapLocation by rememberSaveable { mutableStateOf(false) }

    Box{
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.mapWithDynamicEntities,
            mapViewProxy = viewModel.mapViewProxy,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                viewModel.clearTapLocationAndGeoElement()
                viewModel.setTapLocation(singleTapConfirmedEvent.mapPoint, nullTapLocation)
                viewModel.identifyOnDynamicEntity(singleTapConfirmedEvent)
            },
            content = if (selectedGeoElement != null && calloutVisibility) {
                {
                    Callout(
                        geoElement = selectedGeoElement,
                        modifier = Modifier.wrapContentSize(),
                        tapLocation = viewModel.tapLocation.value,
                        onDynamicEntityChangedEvent = {
                                dynamicEntityChangedInfo ->
                               dynamicEntityObservationId = dynamicEntityChangedInfo.receivedObservation?.id ?: 0
                        }
                    ) {
                        key(dynamicEntityObservationId) {
                            Text(
                                """
                            |Vehicle Name: ${selectedGeoElement.attributes["vehiclename"]}
                            |Vehicle type: ${selectedGeoElement.attributes["vehicletype"]}
                            |Speed: ${selectedGeoElement.attributes["speed"]}
                            |Heading: ${selectedGeoElement.attributes["heading"]}
                            |Point: ${(selectedGeoElement as DynamicEntity).attributes["point_x"]},${(selectedGeoElement).attributes["point_y"]}
                            |Location: ${(selectedGeoElement.geometry as Point).x.roundToInt()},${(selectedGeoElement.geometry as Point).y.roundToInt()}
                        """.trimMargin()
                            )
                        }
                    }
                }
            } else {
                null
            }
        )
    }
}
