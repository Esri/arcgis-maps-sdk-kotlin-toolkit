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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

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
    private val mapPoint = Point(-9815243.889962832, 5130551.535605657, SpatialReference(3857))

    @Before
    fun setContent() = runTest {
        val traceToolUsageScenarios = TraceToolUsageScenarios()
        composeTestRule.setContent {
            traceToolUsageScenarios.MapViewWithTraceInBottomSheet(
                map,
                mapviewProxy,
                graphicsOverlay,
                { drawStatusLatch.countDown() }
            ) {
                Trace(traceState = traceState)
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

    /**
     * Given a Trace composable,
     * When a trace configuration is selected and a starting point is added,
     * Then the trace button is enabled, the trace is executed, and the results are displayed.
     *
     * @since 200.6.0
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRunningADownStreamTrace() = runTest {
        // For all the layers in the map to be done drawing to perform identify successfully
        // We wait for the mapview draw status to change to completed
        drawStatusLatch.await(10, TimeUnit.SECONDS)

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText(context.getString(R.string.trace_configuration)),
            timeoutMillis = timeoutMillis
        )

        val traceButton = composeTestRule.onNodeWithText(context.getString(R.string.trace))
        // make sure the trace button is disabled
        traceButton.assertIsNotEnabled()

        val traceConfigurations =
            composeTestRule.onNodeWithText(context.getString(R.string.no_configuration_selected))
        traceConfigurations.performClick()

        val downStreamTrace = composeTestRule.onNodeWithText("Downstream Trace")
        downStreamTrace.performClick()

        val addNewStartingPointButton =
            composeTestRule.onNodeWithText(context.getString(R.string.add_starting_point))
        addNewStartingPointButton.performClick()

        composeTestRule.runOnUiThread {
            runBlocking {
                traceState.addStartingPoint(mapPoint)
            }
        }

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText("Underground Three Phase"),
            timeoutMillis = timeoutMillis
        )
        // make sure the trace button is enabled
        traceButton.assertIsEnabled()
        // execute trace
        traceState.trace()
        // navigate to the results screen
        composeTestRule.runOnUiThread {
            runBlocking {
                traceState.showScreen(TraceNavRoute.TraceResults)
            }
        }
        // wait for the results screen to load
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasText("Downstream Trace 1"),
            timeoutMillis = timeoutMillis
        )
        // make sure the feature results are displayed
        composeTestRule.onNodeWithText(context.getString(R.string.feature_results)).assertExists()
        composeTestRule.onNodeWithText("4").assertExists()

        // make sure the function results are displayed
        composeTestRule.onNodeWithText(context.getString(R.string.function_results)).assertExists()
        composeTestRule.onNodeWithText("6").assertExists()
    }
}
