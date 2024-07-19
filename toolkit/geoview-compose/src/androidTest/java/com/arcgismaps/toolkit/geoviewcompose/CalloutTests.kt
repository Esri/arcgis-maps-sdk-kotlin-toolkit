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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
class CalloutTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<CalloutTestActivity>()

    private val calloutContainerContentDescription = "CalloutContainerLayout"

    @Test
    fun isCalloutDisplayed() = runTest {
        composeTestRule.apply {
            // display Callout
            activity.viewModel.showCallout()

            // verify Callout is displayed
            waitUntil(10000) {
                composeTestRule.onAllNodesWithContentDescription(label = calloutContainerContentDescription)
                    .fetchSemanticsNodes(errorMessageOnFail = "Callout is not displayed").size == 1
            }
        }
    }

    @Test
    fun attemptToDisplayMultipleCallouts() = runTest {

    }

    @Test
    fun testSwitchingBetweenMultipleCallouts() = runTest {

    }

    @Test
    fun testCalloutResetViaStateChanges() = runTest {

    }

    @Test
    fun testCalloutOnRotation() = runTest {

    }

    @Test
    fun testCalloutTheming() = runTest {

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
            when (currentTestCase) {
                CalloutTestCases.isCalloutDisplayed -> {
                    MapView(
                        arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        },
                        content = {
                            if (viewModel.isCalloutVisible) {
                                Callout(location = viewModel.calloutLocation) {
                                    Text(text = "Hello World")
                                }
                            }
                        }
                    )
                }

                CalloutTestCases.attemptToDisplayMultipleCallouts -> TODO()
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

    private val _currentTestCase = MutableStateFlow(CalloutTestCases.isCalloutDisplayed)
    val currentTestCase: StateFlow<CalloutTestCases> = _currentTestCase.asStateFlow()

    //
    var isCalloutVisible by mutableStateOf(false)

    // Point location for California, USA.
    var calloutLocation by mutableStateOf(Point(-117.191, 34.0306, SpatialReference.wgs84()))

    fun showCallout() {
        isCalloutVisible = true
    }


    fun hideCallout() {
        isCalloutVisible = false
    }

}

enum class CalloutTestCases {
    isCalloutDisplayed,
    attemptToDisplayMultipleCallouts,
    testSwitchingBetweenMultipleCallouts,
    testCalloutResetViaStateChanges,
    testCalloutOnRotation,
    testCalloutTheming
}
