/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.ar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.arcgismaps.toolkit.ar.internal.Joyslider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class JoysliderTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a [Joyslider] configured to accumulate a value
     * When it is dragged to the right and held
     * Then the value should increase in proportion with the time held
     */
    @Test
    fun joysliderFullDragHold() = runTest {
        var value = 0F
        val joysliderTestTag = "TestSlider"

        composeTestRule.setContent {
            Joyslider(
                onValueChange = { value += it },
                contentDescription = joysliderTestTag
            )
        }

        val joyslider = composeTestRule.onNodeWithContentDescription(joysliderTestTag)
        joyslider.assertIsDisplayed()
        assert(value == 0F)

        joyslider.performTouchInput {
            down(center)
            moveTo(centerRight)
            advanceEventTime(250)
            up(0)
        }

        assert(value >= 4F)
    }

    /**
     * Given a [Joyslider] configured to accumulate a value
     * When it is dragged midway to the right and held
     * Then the value should increase in proportion with the time held, but by less than
     * if dragged all the way to the right for the same period of time
     */
    @Test
    fun joysliderPartialDragHold() = runTest {
        var value = 0F
        val joysliderTestTag = "TestSlider"

        composeTestRule.setContent {
            Joyslider(
                onValueChange = { value += it },
                contentDescription = joysliderTestTag
            )
        }

        val joyslider = composeTestRule.onNodeWithContentDescription(joysliderTestTag)
        joyslider.assertIsDisplayed()
        assert(value == 0F)

        // perform a full drag for comparison
        joyslider.performTouchInput {
            down(center)
            moveTo(centerRight)
            advanceEventTime(250)
            up(0)
        }

        // store the result, then reset value
        val fullDragValue = value
        value = 0F

        joyslider.performTouchInput {
            val centerRightMid = Offset((centerRight.x + center.x) / 2, centerY)
            down(center)
            moveTo(centerRightMid)
            advanceEventTime(250)
            up(0)
        }

        // partial drag should result in a value ~half a full drag
        assert((0.48..0.52).contains(value/fullDragValue))
    }

    /**
     * Given a [Joyslider] configured to accumulate a value
     * When it is dragged to the right and held, before being released
     * Then the value should increase in proportion with the time held. After it is let go,
     * the value should not change any further.
     */
    @Test
    fun joysliderDragHoldLetGo() = runTest {
        var value = 0F
        val joysliderTestTag = "TestSlider"

        composeTestRule.setContent {
            Joyslider(
                onValueChange = { value += it },
                contentDescription = joysliderTestTag
            )
        }

        val joyslider = composeTestRule.onNodeWithContentDescription(joysliderTestTag)
        joyslider.assertIsDisplayed()
        assert(value == 0F)

        joyslider.performTouchInput {
            down(center)
            moveTo(centerRight)
            move(250)
        }

        assert(value >= 4F)

        joyslider.performTouchInput {
            up(0)
        }

        val pointerUpValue = value

        delay(75)
        assert(value == pointerUpValue)
    }

    /**
     * Given a [Joyslider] configured to accumulate a value
     * When it is dragged to the right and held, then dragged to the left and held for the same period
     * Then the value should increase while held to the right, then decrease back to zero after being held to the left
     */
    @Test
    fun joysliderOneWayThenOther() = runTest {
        var value = 0F
        val joysliderTestTag = "TestSlider"

        composeTestRule.setContent {
            Joyslider(
                onValueChange = { value += it },
                contentDescription = joysliderTestTag
            )
        }

        val joyslider = composeTestRule.onNodeWithContentDescription(joysliderTestTag)
        joyslider.assertIsDisplayed()
        assert(value == 0F)

        joyslider.performTouchInput {
            down(center)
            moveTo(centerRight)
            move(250)
        }

        assert(value >= 4F)

        joyslider.performTouchInput {
            moveTo(centerLeft)
            advanceEventTime(250)
            up(0)
        }

        assert(value == 0F)
    }
}
