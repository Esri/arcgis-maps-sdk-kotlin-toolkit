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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import com.arcgismaps.toolkit.geoviewcompose.GeoViewScope
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutColors
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutShapes
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests for Callout on the [GeoViewScope] class.
 *
 * @since 200.5.0
 */
@OptIn(ExperimentalTestApi::class)
class CalloutTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val calloutContainerLabel = "CalloutContainerLayout"
    private val timeoutMillis = 10000L


    /**
     * GIVEN a Callout is wrapped in a boolean if check
     * WHEN the boolean visibility property changes
     * THEN verify the Callout visibility status on MapView.
     *
     * @since 200.5.0
     */
    @Test
    fun testCalloutVisibility() = runTest {
        val calloutScenarios = CalloutScenarios()
        composeTestRule.setContent {
            calloutScenarios.CalloutVisibility()
        }

        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // hide Callout
        composeTestRule.onNodeWithText(calloutScenarios.toggleButtonLabel).performClick()

        // verify Callout is not displayed
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )
    }

    /**
     * GIVEN three Callouts are wrapped in a boolean if check
     * WHEN the boolean visibility property changes
     * THEN verify the only first Callout is visible on MapView.
     *
     * @since 200.5.0
     */
    @Test
    fun attemptToDisplayMultipleCallouts() = runTest {
        val calloutScenarios = CalloutScenarios()
        composeTestRule.setContent {
            calloutScenarios.AttemptToDisplayMultipleCallouts()
        }

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
    }


    /**
     * GIVEN two Callouts which can switch between each other on a boolean change
     * WHEN the boolean property changes
     * THEN verify that the one expected Callout is visible on MapView.
     *
     * @since 200.5.0
     */
    @Test
    fun testSwitchingBetweenMultipleCallouts() = runTest {
        val calloutScenarios = CalloutScenarios()
        composeTestRule.setContent {
            calloutScenarios.SwitchBetweenMultipleCallouts()
        }

        // verify only one Callout displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )


        // verify first Callout#1 is being displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("CALLOUT#1"),
            timeoutMillis = timeoutMillis
        )

        // verify first Callout#2 is not being displayed
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription("CALLOUT#2"),
            timeoutMillis = timeoutMillis
        )

        // switch Callouts
        composeTestRule.onNodeWithText(calloutScenarios.toggleButtonLabel).performClick()

        // verify first Callout#2 is being displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("CALLOUT#2"),
            timeoutMillis = timeoutMillis
        )

        // verify first Callout#1 is not being displayed
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription("CALLOUT#1"),
            timeoutMillis = timeoutMillis
        )
    }

    /**
     * GIVEN a Callout which is displayed on the MapView
     * WHEN state changes are collected from the MapViewScope
     * THEN verify Callout has been recomposed triggering a reset.
     *
     * @since 200.5.0
     */
    @Test
    fun testCalloutResetViaStateChanges() = runTest {
        val calloutScenarios = CalloutScenarios()
        val mapViewModel = MapViewModel()

        composeTestRule.setContent {
            calloutScenarios.ResetViaStateChanges(mapViewModel)
        }

        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        val initialRecompositionCount = mapViewModel.calloutRecompositionCount.value

        // Collect a state change from within MapViewScope
        // this should trigger the Callout reset()
        composeTestRule.onNodeWithText(calloutScenarios.toggleButtonLabel).performClick()

        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // get the current recomposition count after performing state change
        val currentRecompositionCount = mapViewModel.calloutRecompositionCount.value

        // check to see if Callout was recomposed via a Reset state change in MapViewScope
        assertThat(initialRecompositionCount).isNotEqualTo(currentRecompositionCount)
        composeTestRule.waitForIdle()
    }

    /**
     * GIVEN a Callout which is displayed on the MapView
     * WHEN a [CalloutShapes] with minContentSize and a [CalloutColors] with backgroundColor is applied to the Callout
     * THEN verify Callout displays with the expected theme and shape.
     *
     * @since 200.5.0
     */
    @Test
    fun testCalloutTheming() = runTest {
        val calloutScenarios = CalloutScenarios()
        composeTestRule.setContent {
            calloutScenarios.CalloutTheming()
        }
        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        val calloutContainerNodeInteraction = composeTestRule
            .onNodeWithContentDescription(calloutContainerLabel)

        val calloutBitmap = calloutContainerNodeInteraction.captureToImage().asAndroidBitmap()
        // test if the Callout contains a Red background Color
        assert(calloutBitmap.containsColor(Color.Red.hashCode()))

        val calloutContainerNode = calloutContainerNodeInteraction.fetchSemanticsNode()
        // test Callout Size excluding leader height and border width
        assert(calloutContainerNode.size == IntSize(442, 416))
    }
}

fun Bitmap.containsColor(color: Int): Boolean {
    for (x in 0 until width) {
        for (y in 0 until height) {
            if (getPixel(x, y) == color) {
                return true
            }
        }
    }
    return false
}
