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

package com.arcgismaps.toolkit.utilitynetworks

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TraceToolTests : TraceToolTestRunner(
    url = "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
    itemId = "471eb0bf37074b1fbb972b1da70fb310"
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule
    val composeTestRule = createComposeRule()

    private val timeoutMillis = 10000L

    @Before
    fun setContent() = runTest {
        composeTestRule.setContent {
            val traceToolUsageScenarios = TraceToolUsageScenarios()
            traceToolUsageScenarios.MapViewWithTraceInBottomSheet(map, mapviewProxy, graphicsOverlay) {
                Trace(
                    traceState = traceState
                )
            }
        }
    }

    /**
     * Given a Trace composable
     * When it is rendered
     * Then the top level Surface exists
     *
     * @since 200.6.0
     */
    @Test
    fun testTraceToolSurface() {
        val surface = composeTestRule.onNodeWithContentDescription(context.getString(R.string.trace_component))
        surface.assertExists("the base surface of the Trace tool composable does not exist")
    }

    val mapPoint = Point(-9815341.663288785, 5130279.426830624, SpatialReference(3857))

    /**
     * Given a Trace composable
     * When
     * Then
     *
     * @since 200.6.0
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTrace() = runTest {
//        val surface = composeTestRule.onNodeWithContentDescription(context.getString(R.string.trace_component))
//        surface.assertExists("the base surface of the Trace tool composable does not exist")

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText(context.getString(R.string.trace_configuration)),
            timeoutMillis = timeoutMillis
        )
        val traceConfigurations = composeTestRule.onNodeWithText(context.getString(R.string.no_configuration_selected))
        traceConfigurations.performClick()

        val downStreamTrace = composeTestRule.onNodeWithText("Downstream Trace")
        downStreamTrace.performClick()

        val addNewStartingPointButton = composeTestRule.onNodeWithText(context.getString(R.string.add_starting_point))
        addNewStartingPointButton.performClick()

        withContext(Dispatchers.Default) {
            traceState.addStartingPoint(mapPoint)
        }


//        val cancelAddStartingPointModeButton = composeTestRule.onNodeWithText(context.getString(R.string.cancel_starting_point_selection))
//        cancelAddStartingPointModeButton.performClick()

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText("Underground Single Phase"),
            timeoutMillis = timeoutMillis
        )

        val startingPointNode = composeTestRule.onNodeWithText("Underground Single Phase")
        startingPointNode.assertExists("Starting Point node does not exist")

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText("Downstream"),
            timeoutMillis = timeoutMillis
        )
//        traceConfigurations.onChildAt(0).performClick()
//        composeTestRule.onNodeWithText(context.getString(R.string.add_starting_point)).performClick()



    }

}
