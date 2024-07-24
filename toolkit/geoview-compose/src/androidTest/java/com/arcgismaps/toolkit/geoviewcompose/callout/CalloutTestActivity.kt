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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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


/**
 * This activity is used to test the Callout
 *
 * @since 200.5.0
 */
class CalloutTestActivity : ComponentActivity() {
    val viewModel: CalloutTestViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val currentTestCase = viewModel.currentTestCase.collectAsState().value
            val isCalloutVisible = viewModel.isCalloutVisible.collectAsState().value
            val point = viewModel.calloutLocation.collectAsState().value

            when (currentTestCase) {
                CalloutTestCases.TestCalloutVisibility -> {
                    MapView(
                        arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        },
                        content = {
                            if (isCalloutVisible) {
                                Callout(location = point) {
                                    Text(text = "Hello World")
                                }
                            }
                        }
                    )
                }

                CalloutTestCases.AttemptToDisplayMultipleCallouts -> {
                    MapView(
                        arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        },
                        content = {
                            if (isCalloutVisible) {
                                Callout(location = point) {
                                    Text(
                                        modifier = Modifier.semantics {
                                            contentDescription = "CALLOUT#1"
                                        },
                                        text = "I am Callout #1"
                                    )
                                }
                                Callout(location = point) {
                                    Text(
                                        modifier = Modifier.semantics {
                                            contentDescription = "CALLOUT#2"
                                        },
                                        text = "I am Callout #2"
                                    )
                                }
                                Callout(location = point) {
                                    Text(
                                        modifier = Modifier.semantics {
                                            contentDescription = "CALLOUT#3"
                                        },
                                        text = "I am Callout #3"
                                    )
                                }
                            }
                        }
                    )
                }

                CalloutTestCases.TestSwitchingBetweenMultipleCallouts -> {
                    var isShowingFirstCallout by remember { mutableStateOf(true) }

                    Column {
                        MapView(
                            modifier = Modifier.weight(1f),
                            arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                                initialViewpoint = Viewpoint(
                                    latitude = 39.8,
                                    longitude = -98.6,
                                    scale = 10e7
                                )
                            },
                            content = {
                                if (isCalloutVisible) {
                                    if (isShowingFirstCallout) {
                                        Callout(location = point) {
                                            Text(
                                                modifier = Modifier.semantics {
                                                    contentDescription = "CALLOUT#1"
                                                },
                                                text = "I am Callout #1"
                                            )
                                        }
                                    } else {
                                        Callout(location = point) {
                                            Text(
                                                modifier = Modifier.semantics {
                                                    contentDescription = "CALLOUT#2"
                                                },
                                                text = "I am Callout #2"
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Button(
                            modifier = Modifier.semantics { contentDescription = "SwitchCallouts" },
                            onClick = {
                                isShowingFirstCallout = !isShowingFirstCallout
                            }) {
                            Text(text = "SwitchCallouts")
                        }
                    }
                }

                CalloutTestCases.TestCalloutResetViaStateChanges -> {
                    MapView(
                        arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        },
                        content = {
                            val calloutLocation = viewModel.calloutLocation.collectAsState().value
                            if (isCalloutVisible) {
                                Callout(location = calloutLocation) {
                                    Text(text = "Hello World")
                                }

                                // update recomposition counter
                                viewModel.calloutWasRecomposed()
                            }
                        }
                    )
                }

                CalloutTestCases.TestCalloutTheming -> {
                    MapView(
                        arcGISMap = ArcGISMap(SpatialReference.wgs84()).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        },
                        content = {
                            if (isCalloutVisible) {
                                Callout(
                                    location = point,
                                    shapes = CalloutDefaults.shapes(
                                        minSize = convertIntSizeToDpSize(IntSize(500,500))
                                    ),
                                    colorScheme = CalloutDefaults.colors(
                                        backgroundColor = Color.Red
                                    )
                                ) {
                                    Text(text = "Hello World")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun convertIntSizeToDpSize(intSize: IntSize): DpSize {
        val density = LocalDensity.current
        return with(density) {
            DpSize(intSize.width.toDp(), intSize.height.toDp())
        }
    }
}

/**
 * ViewModel for the [CalloutTestActivity] for testing
 *
 * @since 200.5.0
 *
 */
class CalloutTestViewModel : ViewModel() {

    // Point location for California, USA.
    private val initialPoint = Point(-117.191, 34.0306, SpatialReference.wgs84())

    private val _currentTestCase = MutableStateFlow(CalloutTestCases.TestCalloutVisibility)
    val currentTestCase: StateFlow<CalloutTestCases> = _currentTestCase.asStateFlow()

    private val _calloutLocation = MutableStateFlow(initialPoint)
    val calloutLocation: StateFlow<Point> = _calloutLocation.asStateFlow()

    private val _isCalloutVisible = MutableStateFlow(false)
    val isCalloutVisible: StateFlow<Boolean> = _isCalloutVisible.asStateFlow()

    private val _calloutRecompositionCount = MutableStateFlow(0)
    val calloutRecompositionCount: StateFlow<Int> = _calloutRecompositionCount.asStateFlow()

    fun showCallout() {
        _calloutLocation.value = initialPoint
        _isCalloutVisible.value = true
    }

    fun hideCallout() {
        _isCalloutVisible.value = false
    }

    fun setCurrentTestCase(testCase: CalloutTestCases) {
        _currentTestCase.value = testCase
    }

    fun updatePointToNewLocation() {
        _calloutLocation.value = Point(-117.191, 35.0306, SpatialReference.wgs84())
    }

    fun calloutWasRecomposed() {
        _calloutRecompositionCount.value++
    }
}
