package com.arcgismaps.toolkit.ar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.arcgismaps.toolkit.ar.internal.Joyslider
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
    fun joysliderDragHold() = runTest {
        var value = 0F
        composeTestRule.setContent {
            Joyslider(onValueChange = {value += it})
        }

        val joysliderTestTag = "Slider"
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
     * When it is dragged to the right and held, before being released
     * Then the value should increase in proportion with the time held. After it is let go,
     * the value should not change any further.
     */
    @Test
    fun joysliderDragHoldLetGo() = runTest {
        var value = 0F
        composeTestRule.setContent {
            Joyslider(onValueChange = {value += it})
        }

        val joysliderTestTag = "Slider"
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

        joyslider.performTouchInput {
            advanceEventTime(250)
        }

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
        composeTestRule.setContent {
            Joyslider(onValueChange = {value += it})
        }

        val joysliderTestTag = "Slider"
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

        assert(value==0F)
    }

}
