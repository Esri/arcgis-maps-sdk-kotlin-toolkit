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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.ScalebarViewModel
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tests for ScalebarViewModel.
 *
 * @since 200.7.0
 */
class ScalebarViewModelTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val esriRedlands = Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator())
    private val defaultLabelTypography = LabelTypography(labelStyle = TextStyle(fontSize = 9.sp))

    /**
     * Given a Scalebar view model
     * When the Scalebar of line style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testLineStyle() = runTest {
        testScalebarViewModel(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.Line,
            maxWidth = 175.0,
            units = ScalebarUnits.METRIC,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            labelTypography = defaultLabelTypography,
            displayLength = 171,
            labels = listOf("375 km")
        )
    }

    /**
     * Given a Scalebar view model
     * When the Scalebar of Bar style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testBarStyle() = runTest {
        testScalebarViewModel(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.Bar,
            maxWidth = 175.0,
            units = ScalebarUnits.METRIC,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            labelTypography = defaultLabelTypography,
            displayLength = 171,
            labels = listOf("375 km")
        )
    }

    /**
     * Given a Scalebar view model
     * When the Scalebar of Bar style is updated
     * Then the display length and labels should be correct
     *
     * @since 200.7.0
     */
    @Test
    fun testGraduatedLineStyle() = runTest {
        testScalebarViewModel(
            x = esriRedlands.x,
            y = esriRedlands.y,
            style = ScalebarStyle.GraduatedLine,
            maxWidth = 175.0,
            units = ScalebarUnits.METRIC,
            scale = 10000000.0,
            unitsPerDip = 2645.833333330476,
            labelTypography = defaultLabelTypography,
            displayLength = 137,
            labels = listOf("0", "100", "200", "300 km")
        )
    }

    /**
     * Executes a test for the ScalebarViewModel with the given parameters.
     *
     * @since 200.7.0
     */
    private fun testScalebarViewModel(
        x: Double,
        y: Double,
        spatialReference: SpatialReference = SpatialReference.webMercator(),
        style: ScalebarStyle,
        maxWidth: Double,
        units: ScalebarUnits,
        scale: Double,
        unitsPerDip: Double,
        labelTypography: LabelTypography,
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

        val viewModel = ScalebarViewModel(
            0.0,
            style,
            units,
            labelTypography,
            useGeodeticCalculations
        )

        val lineWidth = 2.0f // this is the value being passed after the available line display length is calculated
                             // in swift, it is calculated as maxWidth - lineWidth
//        val availableLineDisplayLength = maxWidth - lineWidth

        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val availableLineDisplayLength = measureAvailableLineDisplayLength(maxWidth, labelTypography, style)

                viewModel.computeScalebarProperties(
                    spatialReference,
                    viewpoint,
                    unitsPerDip,
                    availableLineDisplayLength
                )
                val isUpdateLabels by viewModel.isUpdateLabels
                if (isUpdateLabels) {
                    viewModel.updateLabels(measureMinSegmentWidth(viewModel.lineMapLength, ScalebarDefaults.typography()))
                }

                assertThat(viewModel.displayLength.roundToInt()).isEqualTo(displayLength)
                assertThat(viewModel.labels.size).isEqualTo(labels.size)
                for (i in labels.indices) {
                    assertThat(viewModel.labels[i].label).isEqualTo(labels[i])
                }
            }
        }
    }
}
