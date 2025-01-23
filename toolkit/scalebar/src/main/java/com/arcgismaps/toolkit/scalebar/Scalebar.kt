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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.LineScalebar
import com.arcgismaps.toolkit.scalebar.internal.ScalebarDivision
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.format
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.toPx
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
    maxDisplayWidth: Double, //  maximum screen width allotted to the scalebar
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

    val availableDisplayLength = measureAvailableDisplayLength(maxDisplayWidth, labelTypography, style)

    val scalebarProperties by remember(spatialReference, viewpoint, unitsPerDip, availableDisplayLength) {
        derivedStateOf {
            calculateScalebarProperties(
                minScale = minScale,
                maxDisplayLength = availableDisplayLength,
                useGeodeticCalculations = useGeodeticCalculations,
                spatialReference = spatialReference,
                viewpoint = viewpoint,
                unitsPerDip = unitsPerDip,
                scalebarUnits = units,
            )
        }
    }

    // Measure the minimum segment width required to display the labels without overlapping
    val minSegmentWidth = measureMinSegmentWidth(scalebarProperties?.displayLength ?: 0.0, labelTypography)
    val scalebarLabels = generateScalebarLabels(
        minSegmentWidth,
        scalebarProperties?.displayLength ?: 0.0,
        scalebarProperties?.lineLength ?: 0.0,
        scalebarProperties?.displayUnit,
        style,
        labelTypography
    )

    if (scalebarLabels.isNotEmpty()) {
        val density = LocalDensity.current
        Scalebar(
            scalebarProperties?.displayLength?.toPx(density) ?: 0.0,
            scalebarLabels,
            style,
            colorScheme,
            shapes,
            modifier
        )
    }
}

@Composable
private fun Scalebar(
    maxWidth: Double,
    labels: List<ScalebarDivision>,
    scalebarStyle: ScalebarStyle,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    modifier: Modifier = Modifier
) {
    when (scalebarStyle) {
        ScalebarStyle.AlternatingBar -> TODO()
        ScalebarStyle.Bar -> TODO()
        ScalebarStyle.DualUnitLine -> TODO()
        ScalebarStyle.GraduatedLine -> TODO()
        ScalebarStyle.Line -> LineScalebar(
            modifier = modifier,
            maxWidth = maxWidth.toFloat(),
            label = labels[0].label,
            colorScheme = colorScheme,
        )
    }
}

@Preview
@Composable
internal fun ScalebarPreview() {
    Scalebar(
        maxDisplayWidth = 200.0,
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
 * @return maxLength to be passed to updateScalebar fun in ScalebarViewModel
 * @since 200.7.0
 */
@Composable
private fun measureAvailableDisplayLength(
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
            val maxUnitDisplayWidth = textMeasurer.measure(" km", labelTypography.labelStyle).size.width
            maxWidth - (lineWidth / 2.0f) - maxUnitDisplayWidth
        }
        ScalebarStyle.Bar,
        ScalebarStyle.Line -> {
            maxWidth - lineWidth
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
    scalebarLineLength: Double,
    labelTypography: LabelTypography
): Double {
    // The constraining factor is the space required to draw the labels. Create a testString containing the longest
    // label, which is usually the one for 'distance' because the other labels will be smaller numbers.
    // But if 'distance' is small some of the other labels may use decimals, so allow for each label needing at least
    // 3 characters
    val minSegmentTestString: String = if (scalebarLineLength >= 100) {
        scalebarLineLength.toInt().toString()
    } else {
        "9.9"
    }
    // Calculate the bounds of the testString to determine its length
    val textMeasurer = rememberTextMeasurer()
    val maxUnitDisplayWidth = textMeasurer.measure(minSegmentTestString, labelTypography.labelStyle).size.width
    // Calculate the minimum segment length to ensure the labels don't overlap; multiply the testString length by 1.5
    // to allow for the right-most label being right-justified whereas the other labels are center-justified
    return (maxUnitDisplayWidth * 1.5) + (labelXPadding * 2)
}

/**
 * Computes the Scalebar properties namely DisplayLength, DisplayUnit and LineMapLength
 * with the new values of the given parameters.
 *
 * @since 200.7.0
 */
internal fun calculateScalebarProperties(
    minScale: Double,
    maxDisplayLength: Double,
    useGeodeticCalculations: Boolean,
    scalebarUnits: ScalebarUnits,
    spatialReference: SpatialReference?,
    viewpoint: Viewpoint?,
    unitsPerDip: Double?,
): ScalebarProperties? {
    if (spatialReference == null || unitsPerDip == null || viewpoint == null) {
        return null
    }

    if (minScale > 0 && viewpoint.targetScale >= minScale || unitsPerDip.isNaN()) {
        return null
    }

    val mapCenter = viewpoint.targetGeometry.extent.center

    val localDisplayLength: Double
    val localDisplayUnit: LinearUnit
    val localScalebarLineLength: Double

    if (useGeodeticCalculations || spatialReference.unit is AngularUnit) {
        val maxLengthPlanar = unitsPerDip * maxDisplayLength
        val p1 = Point(
            x = mapCenter.x - (maxLengthPlanar * 0.5),
            y = mapCenter.y,
            spatialReference = spatialReference
        )
        val p2 = Point(
            x = mapCenter.x + (maxLengthPlanar * 0.5),
            y = mapCenter.y,
            spatialReference = spatialReference
        )
        val polyline = Polyline(
            points = listOf(p1, p2),
            spatialReference = spatialReference
        )
        val baseUnits = scalebarUnits.baseLinearUnit
        val maxLengthGeodetic = GeometryEngine.lengthGeodetic(
            polyline,
            baseUnits,
            GeodeticCurveType.Geodesic
        )
        val roundNumberDistance = scalebarUnits.closestDistanceWithoutGoingOver(
            maxLengthGeodetic,
            baseUnits
        )
        val planarToGeodeticFactor = maxLengthPlanar / maxLengthGeodetic
        localDisplayLength = (roundNumberDistance * planarToGeodeticFactor) / unitsPerDip
        localDisplayUnit = scalebarUnits.linearUnitsForDistance(roundNumberDistance)
        localScalebarLineLength = baseUnits.convertTo(localDisplayUnit, roundNumberDistance)
    } else {
        val srUnit = spatialReference.unit as? LinearUnit ?: return null
        val baseUnits = scalebarUnits.baseLinearUnit
        val lenAvail = srUnit.convertTo(
            baseUnits,
            unitsPerDip * maxDisplayLength
        )
        val closestLength = scalebarUnits.closestDistanceWithoutGoingOver(
            lenAvail,
            baseUnits
        )
        localDisplayLength = baseUnits.convertTo(
            srUnit,
            closestLength
        ) / unitsPerDip
        localDisplayUnit = scalebarUnits.linearUnitsForDistance(closestLength)
        localScalebarLineLength = baseUnits.convertTo(
            localDisplayUnit,
            closestLength
        )
    }

    if (!localDisplayLength.isFinite() || localDisplayLength.isNaN()) {
        return null
    }
    return ScalebarProperties(
        displayLength = localDisplayLength,
        displayUnit = localDisplayUnit,
        lineLength = localScalebarLineLength
    )
}

/**
 * Updates the labels for the Scalebar.
 *
 * @since 200.7.0
 */
internal fun generateScalebarLabels(
    minSegmentWidth: Double,
    displayLength: Double,
    scalebarLineLength: Double,
    displayUnit: LinearUnit?,
    style: ScalebarStyle,
    labelTypography: LabelTypography,
): List<ScalebarDivision> {
    val suggestedNumSegments = (displayLength / minSegmentWidth).toInt()

    // Cap segments at 4
    val maxNumSegments = minOf(suggestedNumSegments, 4)

    val numSegments = ScalebarUtils.numSegments(
        scalebarLineLength,
        maxNumSegments
    )

    val segmentScreenLength = displayLength / numSegments
    var currSegmentX = 0.0
    val localLabels = mutableListOf<ScalebarDivision>()

    localLabels.add(
        ScalebarDivision(
            index = -1,
            xOffset = 0.0,
            labelYOffset = labelTypography.labelStyle.fontSize.value / 2.0,
            label = "0"
        )
    )

    for (index in 0 until numSegments) {
        currSegmentX += segmentScreenLength
        val segmentMapLength: Double =
            (segmentScreenLength * (index + 1) / displayLength) * scalebarLineLength

        val segmentText: String =
            if (index == numSegments - 1 && displayUnit != null) {
                val displayUnitAbbr = displayUnit.getAbbreviation()
                "${segmentMapLength.format()} $displayUnitAbbr"
            } else {
                segmentMapLength.format()
            }

        val label = ScalebarDivision(
            index = index,
            xOffset = currSegmentX,
            labelYOffset = labelTypography.labelStyle.fontSize.value / 2.0,
            label = segmentText
        )
        localLabels.add(label)
    }

    return if (style == ScalebarStyle.Bar || style == ScalebarStyle.Line) {
        // return a list with the last label or an empty list if there are no labels
        localLabels.takeLast(1)
    } else {
        localLabels
    }
}

internal data class ScalebarProperties(
    val displayLength: Double,
    val displayUnit: LinearUnit,
    val lineLength: Double
)

/**
 * Gets the abbreviation for the LinearUnit.
 *
 * @since 200.7.0
 */
private fun LinearUnit.getAbbreviation(): String {
    return when (this) {
        LinearUnit.meters -> "m"
        LinearUnit.kilometers -> "km"
        LinearUnit.feet -> "ft"
        LinearUnit.miles -> "mi"
        else -> ""
    }
}


