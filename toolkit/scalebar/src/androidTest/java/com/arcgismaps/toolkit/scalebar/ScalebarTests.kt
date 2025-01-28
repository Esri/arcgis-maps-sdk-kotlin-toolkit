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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.arcgismaps.toolkit.scalebar.internal.BarScalebar
import com.arcgismaps.toolkit.scalebar.internal.GraduatedLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.LineScalebar
import com.arcgismaps.toolkit.scalebar.internal.ScalebarDivision
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import org.junit.Rule
import org.junit.Test

/**
 * Tests for Scalebar.
 *
 * @since 200.7.0
 */
class ScalebarTests {
    private val lineScalebarTag = "LineScalebar"
    private val graduatedLineScalebarTag = "GraduatedLineScalebar"
    private val barScalebarTag = "BarScalebar"
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testLineScalebarIsDisplayed() {
        // Test the scalebar
        composeTestRule.setContent {
                LineScalebar(
                    maxWidth = 300f,
                    displayLength = 290f,
                    label = "1000 km",
                    colorScheme = ScalebarDefaults.colors(),
                    labelTypography = ScalebarDefaults.typography(),
                    shapes = ScalebarDefaults.shapes()
                )
            }
        composeTestRule.onNodeWithTag(lineScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a graduated line scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testGraduatedLineScalebarIsDisplayed() {
        val maxWidth = 510f
        val displayLength = 500f
        val tickMarks = listOf(
            ScalebarDivision(0, 0.0, 0.0, "0"),
            ScalebarDivision(1, (displayLength / 4.0), 0.0, "25"),
            ScalebarDivision(2, displayLength / 2.0, 0.0, "50"),
            ScalebarDivision(3, (displayLength / 4.0)* 3, 0.0, "75"),
            ScalebarDivision(4, displayLength.toDouble(), 0.0, "100")
        )
        // Test the scalebar
        composeTestRule.setContent {
                GraduatedLineScalebar(
                    maxWidth = maxWidth,
                    displayLength = displayLength,
                    colorScheme = ScalebarDefaults.colors(),
                    tickMarks = tickMarks,
                    labelTypography = ScalebarDefaults.typography(),
                    shapes = ScalebarDefaults.shapes()
                )
        }
        composeTestRule.onNodeWithTag(graduatedLineScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a bar scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testBarScaleBarIsDisplayed(){
        // Test the scalebar
        composeTestRule.setContent {
            BarScalebar(
                maxWidth = 300f,
                displayLength = 290f,
                label = "1000 km",
                colorScheme = ScalebarDefaults.colors(),
                shapes = ScalebarDefaults.shapes(),
                labelTypography = ScalebarDefaults.typography(),
            )
        }
        composeTestRule.onNodeWithTag(barScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a scalebar with the line color set to red
     * When it is displayed
     * Then it should be visible with the red color
     *
     * @since 200.7.0
     */
    @Test
    fun testLineScaleBarColorChange() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                LineScalebar(
                    maxWidth = 300f,
                    displayLength = 290f,
                    label = "1000 km",
                    colorScheme = ScalebarDefaults.colors(lineColor = Color.Red),
                    labelTypography = ScalebarDefaults.typography(),
                    shapes = ScalebarDefaults.shapes()
                )
            }
        }

        val lineScalebarNodeInteraction = composeTestRule
            .onNodeWithTag(lineScalebarTag).assertIsDisplayed()

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
