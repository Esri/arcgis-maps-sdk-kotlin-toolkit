/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.scalebar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.GraduatedLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.BarScalebar
import com.arcgismaps.toolkit.scalebar.internal.LineScalebar
import com.arcgismaps.toolkit.scalebar.internal.ScalebarDivision
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.toPx
import com.arcgismaps.toolkit.scalebar.internal.computeScalebarProperties
import com.arcgismaps.toolkit.scalebar.internal.computeDivisions
import com.arcgismaps.toolkit.scalebar.internal.labelXPadding
import com.arcgismaps.toolkit.scalebar.internal.lineWidth
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.theme.ScalebarColors
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.arcgismaps.toolkit.scalebar.theme.ScalebarShapes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A composable UI component to display a Scalebar.
 * A Scalebar displays the representation of an accurate linear measurement on the map.
 * It provides a visual indication through which users can determine the size of features or
 * the distance between features on a map.
 * // TODO: update documentation
 *
 * @since 200.7.0
 */
@Composable
public fun Scalebar(
    maxWidth: Double, //  maximum screen width allotted to the scalebar
    unitsPerDip: Double,
    viewpoint: Viewpoint?,
    spatialReference: SpatialReference?,
    modifier: Modifier = Modifier,
    autoHideDelay: Duration = 1.75.seconds, // wait time before the scalebar hides itself, -1 means never hide
    minScale: Double = 0.0, // minimum scale to show the scalebar
    useGeodeticCalculations: Boolean = true, // `false` to compute scale without a geodesic curve,
    style: ScalebarStyle = ScalebarStyle.AlternatingBar,
    // TODO: determining the default ScalebarUnit is not tested
    units: ScalebarUnits = if (isMetric()) {
        ScalebarUnits.METRIC
    } else {
        ScalebarUnits.IMPERIAL
    },
    colorScheme: ScalebarColors = ScalebarDefaults.colors(),
    shapes: ScalebarShapes = ScalebarDefaults.shapes(),
    labelTypography: LabelTypography = ScalebarDefaults.typography()
) {

    val availableLineDisplayLength =
        measureAvailableLineDisplayLength(maxWidth, labelTypography, style)

    val scalebarProperties by remember(spatialReference, viewpoint, unitsPerDip, availableLineDisplayLength) {
        mutableStateOf(
            computeScalebarProperties(
                minScale = minScale,
                spatialReference = spatialReference,
                viewpoint = viewpoint,
                unitsPerDip = unitsPerDip,
                maxLength = availableLineDisplayLength,
                useGeodeticCalculations = useGeodeticCalculations,
                units = units,
            )
        )
    }
    // Measure the minimum segment width required to display the labels without overlapping
    val minSegmentWidth = measureMinSegmentWidth(scalebarProperties.scalebarLengthInMapUnits, labelTypography)
    // update the label text and offsets
    val scalebarDivisions = scalebarProperties.computeDivisions(
        minSegmentWidth = minSegmentWidth,
        labelTypography = labelTypography,
        scalebarStyle = style
    )
    // invoked after the scalebar properties displayLength, displayUnit are computed
    // and the labels are updated
    val density = LocalDensity.current
    Scalebar(
        maxWidth = maxWidth,
        displayLength = scalebarProperties.displayLength.toPx(density),
        labels = scalebarDivisions,
        scalebarStyle = style,
        colorScheme = colorScheme,
        shapes = shapes,
        labelTypography = labelTypography,
        modifier = modifier
    )
}

@Composable
private fun Scalebar(
    maxWidth: Double,
    displayLength: Double,
    labels: List<ScalebarDivision>,
    scalebarStyle: ScalebarStyle,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    labelTypography: LabelTypography,
    modifier: Modifier = Modifier
) {
    when (scalebarStyle) {
        ScalebarStyle.AlternatingBar -> TODO()
        ScalebarStyle.Bar -> BarScalebar(
            modifier = modifier,
            maxWidth = maxWidth.toFloat(),
            displayLength = displayLength.toFloat(),
            label = labels[0].label,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.DualUnitLine -> TODO()
        ScalebarStyle.GraduatedLine -> GraduatedLineScalebar(
            modifier = modifier,
            maxWidth = maxWidth.toFloat(),
            displayLength = displayLength.toFloat(),
            tickMarks = labels,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.Line -> LineScalebar(
            modifier = modifier,
            maxWidth = maxWidth.toFloat(),
            displayLength = displayLength.toFloat(),
            label = labels[0].label,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )
    }
}

@Preview
@Composable
internal fun ScalebarPreview() {
    Scalebar(
        maxWidth = 200.0,
        spatialReference = null,
        unitsPerDip = 1.0,
        viewpoint = Viewpoint(0.0, 0.0, 0.0),
    )
}

@Composable
private fun isMetric(): Boolean {
    // TODO implement the actual logic to determine the default ScalebarUnit
    // this is a placeholder implementation
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("isMetric", true)
}

/**
 * Returns the display length in pixels of the Scalebar line.
 *
 * @return maxWidth the maximum width of the Scalebar taking into account the text units
 * @since 200.7.0
 */
@Composable
internal fun measureAvailableLineDisplayLength(
    maxWidth: Double,
    labelTypography: LabelTypography,
    style: ScalebarStyle
): Double {
    return when (style) {
        ScalebarStyle.AlternatingBar,
        ScalebarStyle.DualUnitLine,
        ScalebarStyle.GraduatedLine -> {
            // " km" will render wider than " mi"
            val textMeasurer = rememberTextMeasurer()
            val maxUnitDisplayWidth = with(LocalDensity.current) {
                textMeasurer.measure(
                    " km",
                    labelTypography.labelStyle
                ).size.width.toDp().value
            }
            maxWidth - (lineWidth.value / 2.0f) - maxUnitDisplayWidth
        }

        ScalebarStyle.Bar,
        ScalebarStyle.Line -> {
            maxWidth - lineWidth.value
        }
    }
}

/**
 * Returns the minimum segment width in pixels required to display the labels without overlapping.
 *
 * @return minimum segment width
 * @since 200.7.0
 */
@Composable
internal fun measureMinSegmentWidth(
    scalebarLengthInMapUnits: Double,
    labelTypography: LabelTypography
): Double {
    // The constraining factor is the space required to draw the labels. Create a testString containing the longest
    // label, which is usually the one for 'distance' because the other labels will be smaller numbers.
    // But if 'distance' is small some of the other labels may use decimals, so allow for each label needing at least
    // 3 characters
    val minSegmentTestString: String = if (scalebarLengthInMapUnits >= 100) {
        scalebarLengthInMapUnits.toInt().toString()
    } else {
        "9.9"
    }
    // Calculate the bounds of the testString to determine its length
    val textMeasurer = rememberTextMeasurer()
    val maxUnitDisplayWidth = with(LocalDensity.current) {
        textMeasurer.measure(
            minSegmentTestString,
            labelTypography.labelStyle
        ).size.width.toDp().value
    }
    // Calculate the minimum segment length to ensure the labels don't overlap; multiply the testString length by 1.5
    // to allow for the right-most label being right-justified whereas the other labels are center-justified
    return (maxUnitDisplayWidth * 1.5) + (labelXPadding * 2)
}
