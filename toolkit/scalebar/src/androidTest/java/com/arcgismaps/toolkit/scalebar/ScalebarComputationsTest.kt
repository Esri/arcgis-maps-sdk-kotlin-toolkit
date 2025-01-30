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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.arcgismaps.UnitSystem
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.computeScalebarProperties
import com.arcgismaps.toolkit.scalebar.internal.computeDivisions
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tests for Scalebar computations.
 *
 * @since 200.7.0
 */
class ScalebarComputationsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val esriRedlands = Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator())

    /**
     * Given a Scalebar
     * When the Scalebar of line style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testLineStyle() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.Line,
            maxWidth = 175.0,
            units = UnitSystem.Metric,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 171,
            labels = listOf("375 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of Bar style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testBarStyle() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.Bar,
            maxWidth = 175.0,
            units = UnitSystem.Metric,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 171,
            labels = listOf("375 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of GraduatedLine style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testGraduatedLineStyle() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.GraduatedLine,
            maxWidth = 175.0,
            units = UnitSystem.Metric,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 km")
        )
    }
    /**
     * Given a Scalebar
     * When the Scalebar of Dual unit line style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testDualUnitLineStyle() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.DualUnitLine,
            maxWidth = 175.0,
            units = UnitSystem.Metric,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyle() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.0,
            units = UnitSystem.Metric,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 km")
        )
    }

    /**
     * Executes a test for the Scalebar with the given parameters.
     *
     * @since 200.7.0
     */
    private fun testScalebar(
        x: Double,
        y: Double,
        spatialReference: SpatialReference = SpatialReference.webMercator(),
        style: ScalebarStyle,
        maxWidth: Double,
        units: UnitSystem,
        scale: Double,
        unitsPerDip: Double,
        useGeodeticCalculations: Boolean = true,
        displayLength: Int,
        labels: List<String>,
    ) = runTest {
        val viewpoint = Viewpoint(
            center = Point(
                x = x,
                y = y,
                spatialReference = spatialReference
            ),
            scale = scale
        )

        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val defaultLabelTypography = ScalebarDefaults.typography()

                val availableLineDisplayLength = measureAvailableLineDisplayLength(maxWidth, defaultLabelTypography, style)
                val scalebarProperties = computeScalebarProperties(
                    0.0,
                    spatialReference,
                    viewpoint,
                    unitsPerDip,
                    availableLineDisplayLength,
                    useGeodeticCalculations,
                    units
                )
                val minimumSegmentWidth = measureMinSegmentWidth(scalebarProperties.scalebarLengthInMapUnits, defaultLabelTypography)
                val scalebarLabels = scalebarProperties.computeDivisions(
                    minSegmentWidth = minimumSegmentWidth,
                    labelTypography = defaultLabelTypography,
                    scalebarStyle = style
                )
                assertThat(scalebarProperties.displayLength.roundToInt()).isEqualTo(displayLength)
                assertThat(scalebarLabels.size).isEqualTo(labels.size)
                for (i in labels.indices) {
                    assertThat(scalebarLabels[i].label).isEqualTo(labels[i])
                }
            }
        }
    }
}
