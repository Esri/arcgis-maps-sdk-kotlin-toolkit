/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.scalebar

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test


/**
 * Tests for Scalebar.
 *
 * @since 200.6.0
 */
class ScalebarTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test the scalebar is displayed.
     * @since 200.6.0
     */
    @Test
    fun testScaleLabelIsDisplayed() {
        // Test the scalebar
        composeTestRule.setContent {
            LineScalebar(
                scaleValue = "1000 km"
            )
        }

        composeTestRule.onNodeWithTag("LineScalebar").assertIsDisplayed()
    }


    /**
     * Test the scalebar is displayed.
     * @since 200.6.0
     */
    @Test
    fun testScaleBarIsDisplayed() {
        composeTestRule.setContent {
            LineScalebar(
                scaleValue = "1000 km",
                lineColor = Color.Red
            )
        }

        val lineScalebarNodeInteraction = composeTestRule
            .onNodeWithTag("LineScalebar")

        val scalebarBitmap = lineScalebarNodeInteraction.captureToImage().asAndroidBitmap()
        // test if the scalebar contains red color
        assert(scalebarBitmap.containsColor(Color.Red.hashCode()))
    }
}


private fun Bitmap.containsColor(color: Int): Boolean {
    for (x in 0 until width) {
        for (y in 0 until height) {
            if (getPixel(x, y) == color) {
                return true
            }
        }
    }
    return false
}
