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

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
        viewModel.setCurrentTestCase(CalloutTestCases.TestCalloutVisibility)
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
        viewModel.setCurrentTestCase(CalloutTestCases.AttemptToDisplayMultipleCallouts)
        // display Callout
        viewModel.showCallout()

        composeTestRule.waitForIdle()

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

        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.TestSwitchingBetweenMultipleCallouts)
        // display Callout
        viewModel.showCallout()
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

        // Switch Callouts
        composeTestRule.onNode(
            matcher = hasContentDescription("SwitchCallouts")
        ).performClick()

        composeTestRule.waitForIdle()

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

        // hide the Callout
        viewModel.hideCallout()
        // verify no Callout is visible
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )
    }

    @Test
    fun testCalloutResetViaStateChanges() = runTest {
        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.TestCalloutResetViaStateChanges)
        // display Callout
        viewModel.showCallout()
        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // get the current recomposition count for the initial Callout
        val initialRecompositionCount = viewModel.calloutRecompositionCount.value

        // Collect a state change from within MapViewScope
        // this should trigger the Callout reset()
        viewModel.updatePointToNewLocation()

        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // check to see if Callout was recomposed via a Reset state change in MapViewScope
        assert(initialRecompositionCount != viewModel.calloutRecompositionCount.value)

        // hide Callout
        viewModel.hideCallout()
        // verify Callout is hidden
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )
    }

    @Test
    fun testCalloutOnRotation() = runTest {
        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.TestCalloutOnRotation)

        // set orientation to Portrait
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
            setOrientationPortrait()
        }

        // display Callout
        viewModel.showCallout()
        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // Rotate to landscape mode
        uiDevice.setOrientationLandscape()
        composeTestRule.waitForIdle()

        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        // Rotate to Portrait mode
        uiDevice.setOrientationPortrait()
        composeTestRule.waitForIdle()

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
    fun testCalloutTheming() = runTest {
        val viewModel = composeTestRule.activity.viewModel
        // set the current test case
        viewModel.setCurrentTestCase(CalloutTestCases.TestCalloutTheming)
        // display Callout
        viewModel.showCallout()
        // verify Callout is displayed
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )

        val calloutContainerNode =
            composeTestRule.onAllNodesWithContentDescription(calloutContainerLabel).onFirst()
                .fetchSemanticsNode()

        assert(calloutContainerNode.layoutInfo.getModifierInfo()
            .filter { it.modifier == Modifier.background(Color.Red) }.size == 1
        )
        assert(calloutContainerNode.size == IntSize(200, 200))


        // hide Callout
        viewModel.hideCallout()
        // verify Callout is hidden
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasContentDescription(calloutContainerLabel),
            timeoutMillis = timeoutMillis
        )
    }
}

private fun hasBackground(node: SemanticsNode, color: Color): Boolean {
    return node.layoutInfo.getModifierInfo().filter { modifierInfo ->
        modifierInfo.modifier == Modifier.background(color)
    }.size == 1
}

enum class CalloutTestCases {
    TestCalloutVisibility,
    AttemptToDisplayMultipleCallouts,
    TestSwitchingBetweenMultipleCallouts,
    TestCalloutResetViaStateChanges,
    TestCalloutOnRotation,
    TestCalloutTheming
}
