/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.compass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class CompassTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a [Compass]
     * When the rotation is 0 degrees
     * And AutoHide is enabled
     * Then the [Compass] should be hidden and visible again when rotation is not 0 degrees
     */
    @Test
    fun testCompassAutoHide() {
        var rotation by mutableStateOf(90.0)

        composeTestRule.setContent {
            Compass(rotation = rotation)
        }

        val compassTestTag = "CompassButtonIcon"
        val compass = composeTestRule.onNodeWithContentDescription(compassTestTag)
        compass.assertIsDisplayed()
        rotation = 0.0
        // advance the fadeout animation of the compass
        composeTestRule.mainClock.advanceTimeBy(500)
        // the node is removed from the compose tree
        compass.assertDoesNotExist()
        rotation = 90.0
        compass.assertIsDisplayed()
    }

    /**
     * Given a [Compass]
     * When it is tapped
     * Then its onClick event is called
     */
    @Test
    fun testCompassOnClick() {
        var rotation = 90
        composeTestRule.setContent {
            Compass(rotation = 90.0) {
                rotation = 0
            }
        }

        val compassTestTag = "CompassButtonIcon"
        val compass = composeTestRule.onNodeWithContentDescription(compassTestTag)
        compass.assertIsDisplayed()
        compass.performClick()
        assertEquals(rotation, 0)
    }
}
