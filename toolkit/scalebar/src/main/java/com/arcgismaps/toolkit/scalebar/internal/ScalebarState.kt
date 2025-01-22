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
package com.arcgismaps.toolkit.scalebar.internal

import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.ScalebarStyle
import com.arcgismaps.toolkit.scalebar.ScalebarUnits
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.format
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography


internal class ScalebarState(
    private val minScale: Double,
    private val style: ScalebarStyle,
    private val units: ScalebarUnits,
    private val labelTypography: LabelTypography,
    private val useGeodeticCalculations: Boolean
) {
    private val geodeticCurveType: GeodeticCurveType = GeodeticCurveType.Geodesic

    /**
     * Updates the labels for the Scalebar.
     *
     * @since 200.7.0
     */
    internal fun updateLabels(scalebarProperties: ScalebarProperties?, minSegmentWidth: Double): List<ScalebarLabel> {
        if (scalebarProperties == null) {
            return emptyList()
        }
        val suggestedNumSegments = (scalebarProperties.displayLength / minSegmentWidth).toInt()

        // Cap segments at 4
        val maxNumSegments = minOf(suggestedNumSegments, 4)

        val numSegments = ScalebarUtils.numSegments(
            scalebarProperties.lineMapLength,
            maxNumSegments
        )

        val segmentScreenLength = scalebarProperties.displayLength / numSegments
        var currSegmentX = 0.0
        val localLabels = mutableListOf<ScalebarLabel>()

        localLabels.add(
            ScalebarLabel(
                index = -1,
                xOffset = 0.0,
                yOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                text = "0"
            )
        )

        for (index in 0 until numSegments) {
            currSegmentX += segmentScreenLength
            val segmentMapLength: Double =
                (segmentScreenLength * (index + 1) / scalebarProperties.displayLength) * scalebarProperties.lineMapLength

            val segmentText: String =
                if (index == numSegments - 1 /*&& _scalebarProperties.value.displayUnit != null*/) {
                    val displayUnitAbbr = scalebarProperties.displayUnit.getAbbreviation()
                    "${segmentMapLength.format()} $displayUnitAbbr"
                } else {
                    segmentMapLength.format()
                }

            val label = ScalebarLabel(
                index = index,
                xOffset = currSegmentX,
                yOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                text = segmentText
            )
            localLabels.add(label)
        }

        return if (style == ScalebarStyle.Bar || style == ScalebarStyle.Line) {
            if (localLabels.isNotEmpty()) {
                mutableListOf(localLabels.last())
            } else {
                mutableListOf()
            }
        } else {
            localLabels
        }
    }

    /**
     * Computes the Scalebar properties namely DisplayLength, DisplayUnit and LineMapLength
     * with the new values of the given parameters.
     *
     * @since 200.7.0
     */
    internal fun computeScalebarProperties(
        spatialReference: SpatialReference?,
        viewpoint: Viewpoint?,
        unitsPerDip: Double?,
        maxLength: Double,
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
        val localLineMapLength: Double

        if (useGeodeticCalculations || spatialReference.unit is AngularUnit) {
            val maxLengthPlanar = unitsPerDip * maxLength
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
            val baseUnits = units.baseLinearUnit
            val maxLengthGeodetic = GeometryEngine.lengthGeodetic(
                polyline,
                baseUnits,
                geodeticCurveType
            )
            val roundNumberDistance = units.closestDistanceWithoutGoingOver(
                maxLengthGeodetic,
                baseUnits
            )
            val planarToGeodeticFactor = maxLengthPlanar / maxLengthGeodetic
            localDisplayLength = (roundNumberDistance * planarToGeodeticFactor) / unitsPerDip
            localDisplayUnit = units.linearUnitsForDistance(roundNumberDistance)
            localLineMapLength = baseUnits.convertTo(localDisplayUnit, roundNumberDistance)
        } else {
            val srUnit = spatialReference.unit as? LinearUnit ?: return null
            val baseUnits = units.baseLinearUnit
            val lenAvail = srUnit.convertTo(
                baseUnits,
                unitsPerDip * maxLength
            )
            val closestLen = units.closestDistanceWithoutGoingOver(
                lenAvail,
                baseUnits
            )
            localDisplayLength = baseUnits.convertTo(
                srUnit,
                closestLen
            ) / unitsPerDip
            localDisplayUnit = units.linearUnitsForDistance(closestLen)
            localLineMapLength = baseUnits.convertTo(
                localDisplayUnit,
                closestLen
            )
        }

        if (!localDisplayLength.isFinite() || localDisplayLength.isNaN()) {
            return null
        }
        return ScalebarProperties(
            displayLength = localDisplayLength,
            displayUnit = localDisplayUnit,
            lineMapLength = localLineMapLength
        )
    }

}
internal data class ScalebarProperties(
    val displayLength: Double,
    val displayUnit: LinearUnit,
    val lineMapLength: Double
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
