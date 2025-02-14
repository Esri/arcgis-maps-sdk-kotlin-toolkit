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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.AlternatingBarScalebar
import com.arcgismaps.toolkit.scalebar.internal.BarScalebar
import com.arcgismaps.toolkit.scalebar.internal.DualUnitLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.GraduatedLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.LineScalebar
import com.arcgismaps.toolkit.scalebar.internal.ScalebarDivision
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for Scalebar.
 *
 * @since 200.7.0
 */
class ScalebarTests {
    private val lineScalebarTag = "LineScalebar"
    private val graduatedLineScalebarTag = "GraduatedLineScalebar"
    private val barScalebarTag = "BarScalebar"
    private val alternatingBarScalebarTag = "AlternatingBarScalebar"
    private val dualUnitLineScalebarTag = "DualUnitLineScalebar"
    private val scalebarTag = "Scalebar"
    private val esriRedlands = Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator())

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
                    displayLength = 160.0,
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
        val maxWidth = 175.dp
        val displayLength = 139.3
        val tickMarks = listOf(
            ScalebarDivision(0.0, "0"),
            ScalebarDivision((displayLength / 4.0), "25"),
            ScalebarDivision(displayLength / 2.0, "50"),
            ScalebarDivision((displayLength / 4.0) * 3, "75"),
            ScalebarDivision(displayLength.toDouble(), "100")
        )
        // Test the scalebar
        composeTestRule.setContent {
            GraduatedLineScalebar(
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
     * Given a scalebar with a given minScale value
     * When the initial viewpoint is at the same minScale
     * Then the scalebar should not be drawn
     * When the viewpoint is changed to a lower scale
     * Then the scalebar should be shown
     *
     * @since 200.7.0
     */
    @Test
    fun testScalebarMinScale() {
        val minScale = 3125000.0
        val viewPoint = mutableStateOf(Viewpoint(esriRedlands, minScale))
        composeTestRule.setContent {
            Scalebar(
                minScale = minScale,
                modifier = Modifier.testTag(scalebarTag),
                maxWidth = 175.dp,
                unitsPerDip = 2645.833333330476,
                viewpoint = viewPoint.value,
                spatialReference = SpatialReference.webMercator(),
                style = ScalebarStyle.Line,
            )
        }
        composeTestRule.onNodeWithTag(scalebarTag).assertIsNotDisplayed()
        composeTestRule.runOnUiThread {
            viewPoint.value = Viewpoint(esriRedlands, minScale - 1000)
        }
        composeTestRule.onNodeWithTag(scalebarTag).assertIsDisplayed()
    }

    /**
     * Given a bar scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testBarScaleBarIsDisplayed() {
        // Test the scalebar
        composeTestRule.setContent {
            BarScalebar(
                displayLength = 160.0,
                label = "1000 km",
                colorScheme = ScalebarDefaults.colors(),
                shapes = ScalebarDefaults.shapes(),
                labelTypography = ScalebarDefaults.typography(),
            )
        }
        composeTestRule.onNodeWithTag(barScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a dual unit line scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testDualUnitLineScalebarIsDisplayed() {
        val maxWidth = 175.dp
        val displayLength = 139.3
        val endScalebarDivision = ScalebarDivision(displayLength, "3000 mi")
        val alternateScalebarDivision = ScalebarDivision(0.75 * (displayLength),"3500 Km")
        // Test the scalebar
        composeTestRule.setContent {
            DualUnitLineScalebar(
                primaryScalebarDivision = endScalebarDivision,
                alternateScalebarDivision = alternateScalebarDivision,
                colorScheme = ScalebarDefaults.colors(),
                labelTypography = ScalebarDefaults.typography(),
                shapes = ScalebarDefaults.shapes()
            )
        }
        composeTestRule.onNodeWithTag(dualUnitLineScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a AlternatingBar scalebar
     * When it is displayed
     * Then it should be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarScaleBarIsDisplayed() {
        // Test the scalebar
        val maxWidth = 175.dp
        val displayLength = 139.3
        val scalebarDivisions = listOf(
            ScalebarDivision(0.0, "0"),
            ScalebarDivision((displayLength / 3.0), "100"),
            ScalebarDivision(2.0 * displayLength / 3.0, "200"),
            ScalebarDivision(displayLength, "300 km")
        )
        // Test the scalebar
        composeTestRule.setContent {
            AlternatingBarScalebar(
                displayLength = displayLength,
                scalebarDivisions = scalebarDivisions,
                colorScheme = ScalebarDefaults.colors(),
                shapes = ScalebarDefaults.shapes(),
                labelTypography = ScalebarDefaults.typography(),
            )
        }
        composeTestRule.onNodeWithTag(alternatingBarScalebarTag).assertIsDisplayed()
    }

    /**
     * Given a scalebar with an auto-hide delay of 1 second
     * When it is displayed on the screen
     * And a one second timer is reached
     * Then the scalebar should not be visible
     *
     * @since 200.7.0
     */
    @Test
    fun testScalebarAnimation() {
        // Test the scalebar
        val viewPoint = Viewpoint(
            Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator()),
            10000000.0
        )
        composeTestRule.setContent {
            Scalebar(
                modifier = Modifier.testTag(scalebarTag),
                maxWidth = 175.dp,
                unitsPerDip = 2645.833333330476,
                viewpoint = viewPoint,
                spatialReference = SpatialReference.webMercator(),
                style = ScalebarStyle.Line,
                autoHideDelay = 1.seconds
            )
        }
        composeTestRule.onNodeWithTag(scalebarTag).assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag(scalebarTag).assertDoesNotExist()
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
                    displayLength = 160.0,
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
