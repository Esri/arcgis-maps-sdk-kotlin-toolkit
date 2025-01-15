package com.arcgismaps.toolkit.scalebar

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.ScalebarViewModel
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tests for ScalebarViewModel.
 *
 * @since 200.7.0
 */
class ScalebarViewModelTests {

    private val esriRedlands = Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator())
    private val defaultLabelTypography = LabelTypography(labelStyle = TextStyle(fontSize = 11.sp))

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

        val availableLineDisplayLength = maxWidth - 2 // this is the value being passed after the available line display length is calculated
                                                      // in swift, it is calculated as maxWidth - lineWidth
        viewModel.updateScaleBar(spatialReference, viewpoint, unitsPerDip, availableLineDisplayLength)

        assertThat(viewModel.displayLength.roundToInt()).isEqualTo(displayLength)
        assertThat(viewModel.labels.size).isEqualTo(labels.size)
        for (i in labels.indices) {
            assertThat(viewModel.labels[i].text).isEqualTo(labels[i])
        }
    }
}
