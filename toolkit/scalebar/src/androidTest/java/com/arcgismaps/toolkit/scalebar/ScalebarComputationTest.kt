package com.arcgismaps.toolkit.scalebar/*
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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ScalebarCalculationsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val esriRedlands = Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator())
    private val defaultLabelTypography = LabelTypography(labelStyle = TextStyle(fontSize = 11.sp))

    /**
     * Given map properties and device properties
     * When the properties of a line style scalebar are computed
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testLineStyle() {
        testScalebarCalculations(
            point = esriRedlands,
            style = ScalebarStyle.Line,
            maxWidth = 175.0,
            units = ScalebarUnits.METRIC,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            labelTypography = defaultLabelTypography,
            expectedDisplayLength = 171.0,
            expectedLabels = listOf("375 km")
        )
    }

    /**
     * Given map properties and device properties
     * When the properties of a bar scalebar are calculated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testBarStyle() = runTest {
        testScalebarCalculations(
            point = esriRedlands,
            style = ScalebarStyle.Bar,
            maxWidth = 175.0,
            units = ScalebarUnits.METRIC,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            labelTypography = defaultLabelTypography,
            expectedDisplayLength = 171.0,
            expectedLabels = listOf("375 km")
        )
    }


    /**
     * Executes the scalebar calculations and verifies the results.
     *
     * @since 200.7.0
     */
    private fun testScalebarCalculations(
        point: Point,
        style: ScalebarStyle,
        maxWidth: Double,
        units: ScalebarUnits,
        scale: Double,
        unitsPerDip: Double,
        labelTypography: LabelTypography,
        expectedDisplayLength: Double,
        expectedLabels: List<String>,
        spatialReference: SpatialReference = SpatialReference.webMercator(),
        useGeodeticCalculations: Boolean = true,
    ) {
        composeTestRule.setContent {

            val viewpoint = Viewpoint(
                center = Point(
                    x = point.x,
                    y = point.y,
                    spatialReference = spatialReference
                ),
                scale = scale
            )
            val scalebarProperties = computeScalebarProperties(
                minScale = 0.0,
                useGeodeticCalculations = useGeodeticCalculations,
                units = units,
                spatialReference = spatialReference,
                viewpoint = viewpoint,
                unitsPerDip = unitsPerDip,
                maxLength = maxWidth,
            )
            val minSegmentWidth = measureMinSegmentWidth(
                lineMapLength = scalebarProperties?.lineMapLength ?: 0.0,
                labelTypography = labelTypography,
            )
            val scalebarLabels = updateLabels(
                minSegmentWidth = minSegmentWidth,
                displayLength = scalebarProperties?.displayLength ?: 0.0,
                lineMapLength = scalebarProperties?.lineMapLength ?: 0.0,
                displayUnit = scalebarProperties?.displayUnit,
                style = style,
                labelTypography = labelTypography,
            )

            assertThat(scalebarProperties?.displayLength).isWithin(.5).of(expectedDisplayLength)
            assertThat(scalebarLabels.size).isEqualTo(expectedLabels.size)
            for (i in expectedLabels.indices) {
                assertThat(scalebarLabels[i].text).isEqualTo(expectedLabels[i])
            }
        }
    }
}
