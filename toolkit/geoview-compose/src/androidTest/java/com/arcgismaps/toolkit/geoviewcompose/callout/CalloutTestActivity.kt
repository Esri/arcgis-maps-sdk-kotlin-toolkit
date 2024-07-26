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

package com.arcgismaps.toolkit.geoviewcompose.callout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val initialPoint = Point(-117.0, 34.0, SpatialReference.wgs84())
private val secondaryPoint = Point(-120.0, 36.0, SpatialReference.wgs84())

class MapViewModel : ViewModel() {
    private val _calloutLocation = MutableStateFlow(initialPoint)
    val calloutLocation: StateFlow<Point> = _calloutLocation.asStateFlow()
    fun updatePointToNewLocation() {
        _calloutLocation.value = secondaryPoint
    }
}


class CalloutScenarios {

    val toggleButtonLabel = "ToggleCallout"
    val recompositionCounterLabel = "RecompositionCounter"

    @Composable
    fun CalloutVisibility() {
        var isCalloutVisible by remember { mutableStateOf(true) }
        Column {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                    initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
                },
                content = {
                    if (isCalloutVisible) {
                        Callout(location = initialPoint) {
                            Text(text = "Hello World")
                        }
                    }
                }
            )
            Button(onClick = { isCalloutVisible = !isCalloutVisible }) {
                Text(text = toggleButtonLabel)
            }
        }
    }

    @Composable
    fun AttemptToDisplayMultipleCallouts() {
        MapView(
            arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            },
            content = {
                Callout(location = initialPoint) {
                    Text(
                        modifier = Modifier.semantics { contentDescription = "CALLOUT#1" },
                        text = "I am Callout #1"
                    )
                }
                Callout(location = initialPoint) {
                    Text(
                        modifier = Modifier.semantics { contentDescription = "CALLOUT#2" },
                        text = "I am Callout #2"
                    )
                }
                Callout(location = initialPoint) {
                    Text(
                        modifier = Modifier.semantics { contentDescription = "CALLOUT#3" },
                        text = "I am Callout #3"
                    )
                }
            }
        )
    }

    @Composable
    fun SwitchBetweenMultipleCallouts() {
        var isShowingFirstCallout by remember { mutableStateOf(true) }

        Column {
            MapView(
                modifier = Modifier.weight(1f),
                arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                    initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
                },
                content = {
                    if (isShowingFirstCallout) {
                        Callout(location = initialPoint) {
                            Text(
                                modifier = Modifier.semantics { contentDescription = "CALLOUT#1" },
                                text = "I am Callout #1"
                            )
                        }
                    } else {
                        Callout(location = secondaryPoint) {
                            Text(
                                modifier = Modifier.semantics { contentDescription = "CALLOUT#2" },
                                text = "I am Callout #2"
                            )
                        }
                    }
                }
            )

            Button(
                onClick = { isShowingFirstCallout = !isShowingFirstCallout }) {
                Text(text = toggleButtonLabel)
            }
        }
    }


    @Composable
    fun ResetViaStateChanges(mapViewModel: MapViewModel) {
        var calloutRecompositionCount by rememberSaveable { mutableStateOf(0) }

        Column {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                    initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
                },
                content = {
                    val calloutLocation = mapViewModel.calloutLocation.collectAsState().value
                    Callout(location = calloutLocation) {
                        Text(text = "Hello World")
                    }
                    // update recomposition counter
                    calloutRecompositionCount++
                }
            )
            Text(
                modifier = Modifier.testTag(recompositionCounterLabel),
                text = calloutRecompositionCount.toString()
            )
            Button(onClick = { mapViewModel.updatePointToNewLocation() }) {
                Text(text = toggleButtonLabel)
            }
        }
    }

    @Composable
    fun CalloutTheming() {
        MapView(
            arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                initialViewpoint = Viewpoint(
                    latitude = 39.8,
                    longitude = -98.6,
                    scale = 10e7
                )
            },
            content = {
                Callout(
                    location = initialPoint,
                    shapes = CalloutDefaults.shapes(
                        minSize = convertIntSizeToDpSize(IntSize(500, 500))
                    ),
                    colorScheme = CalloutDefaults.colors(
                        backgroundColor = Color.Red
                    )
                ) {
                    Text(text = "Hello World")
                }
            })
    }

    @Composable
    fun convertIntSizeToDpSize(intSize: IntSize): DpSize {
        val density = LocalDensity.current
        return with(density) {
            DpSize(intSize.width.toDp(), intSize.height.toDp())
        }
    }
}
