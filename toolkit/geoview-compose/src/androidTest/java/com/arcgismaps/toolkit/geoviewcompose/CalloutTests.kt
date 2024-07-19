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

package com.arcgismaps.toolkit.geoviewcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


/**
 * Tests the Callout
 *
 * @since 200.5.0
 */
@OptIn(ExperimentalTestApi::class)
class CalloutTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<CalloutTestActivity>()

    private val calloutContainerLabel = "CalloutContainerLayout"
    private val timeoutMillis = 10000L

    @Test
    fun testCalloutVisibility() = runTest {
        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.testCalloutVisibility)
        // display Callout
        viewModel.showCallout()
        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // hide Callout
        viewModel.hideCallout()
        // verify Callout is hidden
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

    }

    @Test
    fun attemptToDisplayMultipleCallouts() = runTest {
        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.attemptToDisplayMultipleCallouts)
        // display Callout
        viewModel.showCallout()
        // verify only one Callout displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // verify first Callout is being displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("CALLOUT#1"),
            timeoutMillis = timeoutMillis
        )

        // verify other two Callouts are not being displayed
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription("CALLOUT#2"),
            timeoutMillis = timeoutMillis
        )
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription("CALLOUT#3"),
            timeoutMillis = timeoutMillis
        )

        // hide the Callout
        viewModel.hideCallout()
        // verify no Callout is visible
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )
    }

    @Test
    fun testSwitchingBetweenMultipleCallouts() = runTest {
        // TODO
    }

    @Test
    fun testCalloutResetViaStateChanges() = runTest {
        // TODO
    }

    @Test
    fun testCalloutOnRotation() = runTest {
        // TODO
    }

    @Test
    fun testCalloutTheming() = runTest {
        // TODO
    }


}


/**
 * This activity is used to test the Callout
 *
 * @since 200.5.0
 */
class CalloutTestActivity() : ComponentActivity() {
    val viewModel: CalloutTestViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {

            val currentTestCase = viewModel.currentTestCase.collectAsState().value
            val isCalloutVisible = viewModel.isCalloutVisible.collectAsState().value
            val point = viewModel.calloutLocation.collectAsState().value

            when (currentTestCase) {
                CalloutTestCases.testCalloutVisibility -> {
                    MapView(
                        arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
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

                CalloutTestCases.attemptToDisplayMultipleCallouts -> {
                    MapView(
                        arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
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

                CalloutTestCases.testSwitchingBetweenMultipleCallouts -> TODO()
                CalloutTestCases.testCalloutResetViaStateChanges -> TODO()
                CalloutTestCases.testCalloutOnRotation -> TODO()
                CalloutTestCases.testCalloutTheming -> TODO()
            }

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

    private val _currentTestCase = MutableStateFlow(CalloutTestCases.testCalloutVisibility)
    val currentTestCase: StateFlow<CalloutTestCases> = _currentTestCase.asStateFlow()

    private val _calloutLocation = MutableStateFlow(initialPoint)
    val calloutLocation: StateFlow<Point> = _calloutLocation.asStateFlow()

    private val _isCalloutVisible = MutableStateFlow(false)
    val isCalloutVisible: StateFlow<Boolean> = _isCalloutVisible.asStateFlow()

    fun showCallout() {
        _isCalloutVisible.value = true
    }


    fun hideCallout() {
        _isCalloutVisible.value = false
    }

    fun setCurrentTestCase(testCase: CalloutTestCases) {
        _currentTestCase.value = testCase
    }

}

enum class CalloutTestCases {
    testCalloutVisibility,
    attemptToDisplayMultipleCallouts,
    testSwitchingBetweenMultipleCallouts,
    testCalloutResetViaStateChanges,
    testCalloutOnRotation,
    testCalloutTheming
}
