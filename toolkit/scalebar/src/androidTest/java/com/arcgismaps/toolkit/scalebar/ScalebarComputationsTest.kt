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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.UnitSystem
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.computeDivisions
import com.arcgismaps.toolkit.scalebar.internal.computeScalebarProperties
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
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
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
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
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
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
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
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 137,
            labels = listOf("300 km", "175 mi")
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
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with Imperial units
     * Then the display length and labels should be correct
     * And the labels should be in miles
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleImperial() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Imperial,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 147,
            labels = listOf("0", "100", "200 mi")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with Geodetic calculations disabled
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleDisableGeodetic() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            useGeodeticCalculations = false,
            displayLength = 142,
            labels = listOf("0", "125", "250", "375 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a width of 100 dp
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleWidth_100() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 100.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 68,
            labels = listOf("0", "150 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a width of 300 dp
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleWidth_300() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 300.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 273,
            labels = listOf("0", "200", "400", "600 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a width of 500 dp
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleWidth_500() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 500.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 456,
            labels = listOf("0", "250", "500", "750", "1000 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a view centered near the Arctic Ocean
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleArcticOcean() = runTest {
        testScalebar(
            x = -24752697.0,
            y = 15406913.0,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 128,
            labels = listOf("0", "20", "40", "60 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a view centered near the Antarctica
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleNearAntarctica() = runTest {
        testScalebar(
            x = -35729271.0,
            y = -13943757.0,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10_000_000.0,
            unitsPerDip = 2645.833333330476,
            displayLength = 136,
            labels = listOf("0", "40", "80 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 100
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_100() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 100.0,
            unitsPerDip = 0.02645833333330476,
            displayLength = 137,
            labels = listOf("0", "1", "2", "3 m")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 1000
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_1000() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 1000.0,
            unitsPerDip = 0.26458333333304757,
            displayLength = 137,
            labels = listOf("0", "10", "20", "30 m")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 10000
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_10000() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 10000.0,
            unitsPerDip = 2.6458333333304758,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 m")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 100000
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_100000() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 100000.0,
            unitsPerDip = 26.458333333304758,
            displayLength = 137,
            labels = listOf("0", "1", "2", "3 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 1000000
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_1000000() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 1000000.0,
            unitsPerDip = 264.58333333304756,
            displayLength = 137,
            labels = listOf("0", "10", "20", "30 km")
        )
    }

    /**
     * Given a Scalebar
     * When the Scalebar of AlternatingBar style is created with a scale of 80,000,000
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testAlternatingBarStyleScale_80000000() = runTest {
        testScalebar(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.AlternatingBar,
            maxWidth = 175.dp,
            units = UnitSystem.Metric,
            scale = 80000000.0,
            unitsPerDip = 21166.666666643807,
            displayLength = 143,
            labels = listOf("0", "1250", "2500 km")
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
        maxWidth: Dp,
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

                val availableLineDisplayLength = measureAvailableLineDisplayLength(maxWidth.value.toDouble(), defaultLabelTypography, style)
                val scalebarProperties = computeScalebarProperties(
                    minScale = 0.0,
                    spatialReference,
                    viewpoint,
                    unitsPerDip,
                    maxLength = availableLineDisplayLength,
                    useGeodeticCalculations,
                    units
                )
                val minimumSegmentWidth =
                    measureMinSegmentWidth(scalebarProperties.scalebarLengthInMapUnits, defaultLabelTypography)
                val scalebarLabels = scalebarProperties.computeDivisions(
                    minSegmentWidth = minimumSegmentWidth,
                    scalebarStyle = style,
                    units = units
                )
                assertThat(scalebarProperties.displayLength.roundToInt()).isEqualTo(displayLength)
                assertThat(scalebarLabels.map { it.label }).containsExactlyElementsIn(labels).inOrder()
            }
        }
    }
}
