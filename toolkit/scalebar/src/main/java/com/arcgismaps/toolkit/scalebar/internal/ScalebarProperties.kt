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

package com.arcgismaps.toolkit.scalebar.internal

import com.arcgismaps.UnitSystem
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.ScalebarStyle
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.baseLinearUnit
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.closestDistanceWithoutGoingOver
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.format
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.linearUnitsForDistance

/**
 * A data class to hold the properties of the Scalebar.
 *
 * @since 200.7.0
 */
internal data class ScalebarProperties(
    val displayLength: Double,
    val mapUnitsToDisplay: LinearUnit,
    val scalebarLengthInMapUnits: Double
) {
    companion object {
        val NOT_INITIALIZED = ScalebarProperties(
            displayLength = 0.0,
            mapUnitsToDisplay = LinearUnit.meters,
            scalebarLengthInMapUnits = 0.0
        )
    }
}

private const val MAX_NUM_OF_SEGMENTS = 4

/**
 * Computes the scalebar divisions based on the given parameters.
 *
 * @param minSegmentWidth The minimum width of a segment.
 * @param scalebarStyle The style of the scalebar.
 * @since 200.7.0
 */
internal fun ScalebarProperties.computeDivisions(
    minSegmentWidth: Double,
    scalebarStyle: ScalebarStyle,
    units: UnitSystem
): List<ScalebarDivision> {
    return when (scalebarStyle) {
        ScalebarStyle.Bar,ScalebarStyle.Line -> {
            listOf(
                createPrimaryDivision(
                    displayUnitAbbreviation = mapUnitsToDisplay.getAbbreviation(),
                    scalebarLengthInMapUnits = scalebarLengthInMapUnits,
                    xOffset = displayLength
                )
            )
        }
        ScalebarStyle.DualUnitLine -> {
            listOf(
                createPrimaryDivision(
                    displayUnitAbbreviation = mapUnitsToDisplay.getAbbreviation(),
                    scalebarLengthInMapUnits = scalebarLengthInMapUnits,
                    xOffset = displayLength
                ),
                computeAlternateUnitScalebarDivision(units)
            )
        }
        else -> {
            val suggestedNumSegments = (displayLength / minSegmentWidth).toInt()
            val maxNumSegments = minOf(suggestedNumSegments, MAX_NUM_OF_SEGMENTS)

            val numSegments = ScalebarUtils.numSegments(
                scalebarLengthInMapUnits,
                maxNumSegments
            )

            val segmentScreenLength = displayLength / numSegments
            var currSegmentX = 0.0
            val localLabels = mutableListOf<ScalebarDivision>()

            // Add the first label at 0
            localLabels.add(
                ScalebarDivision(
                    xOffset = 0.0,
                    label = "0"
                )
            )

            for (index in 1 until numSegments) {
                currSegmentX += segmentScreenLength
                val segmentLengthInMapUnits: Double =
                    (segmentScreenLength * index / displayLength) * scalebarLengthInMapUnits

                val label = ScalebarDivision(
                    xOffset = currSegmentX,
                    label = segmentLengthInMapUnits.format()
                )
                localLabels.add(label)
            }

            localLabels.add(
                createPrimaryDivision(
                    displayUnitAbbreviation = mapUnitsToDisplay.getAbbreviation(),
                    scalebarLengthInMapUnits = scalebarLengthInMapUnits,
                    xOffset = displayLength
                )
            )
            
            localLabels
        }
    }
}

/**
 * Creates the primary scalebar division.
 *
 * It contains the length and [displayUnitAbbreviation].
 * @since 200.7.0
 */
private fun createPrimaryDivision(
    displayUnitAbbreviation: String,
    scalebarLengthInMapUnits: Double,
    xOffset: Double,
): ScalebarDivision {
    val label = ScalebarDivision(
        xOffset = xOffset,
        label = "${scalebarLengthInMapUnits.format()} $displayUnitAbbreviation"
    )
    return label
}

/**
 * Computes the [ScalebarProperties] based on the given parameters.
 *
 * @param minScale The minimum scale for the scalebar to be visible.
 * @param spatialReference The spatial reference of the map.
 * @param viewpoint The viewpoint of the map.
 * @param unitsPerDip The number of units per device independent pixel.
 * @param maxLength The maximum length of the scalebar.
 * @param useGeodeticCalculations Whether to use geodetic calculations.
 * @param units The units for the scalebar.
 * @since 200.7.0
 */
internal fun computeScalebarProperties(
    minScale: Double,
    spatialReference: SpatialReference?,
    viewpoint: Viewpoint?,
    unitsPerDip: Double?,
    maxLength: Double,
    useGeodeticCalculations: Boolean,
    units: UnitSystem
): ScalebarProperties {
    if (spatialReference == null || unitsPerDip == null || viewpoint == null) {
        return ScalebarProperties.NOT_INITIALIZED
    }

    if (minScale > 0 && viewpoint.targetScale >= minScale || unitsPerDip.isNaN()) {
        return ScalebarProperties.NOT_INITIALIZED
    }

    val mapCenter = viewpoint.targetGeometry.extent.center

    val (localDisplayLength, localDisplayUnit, localLineMapLength) = if (useGeodeticCalculations || spatialReference.unit is AngularUnit) {
        calculateGeodeticProperties(mapCenter, spatialReference, unitsPerDip, maxLength, units)
    } else {
        calculatePlanarProperties(spatialReference, unitsPerDip, maxLength, units)
    }

    if (!localDisplayLength.isFinite() || localDisplayLength.isNaN()) {
        return ScalebarProperties.NOT_INITIALIZED
    }

    return ScalebarProperties(
        displayLength = localDisplayLength,
        mapUnitsToDisplay = localDisplayUnit,
        scalebarLengthInMapUnits = localLineMapLength
    )
}

/**
 * Computes the alternate unit scalebar division based on the given parameters for the dual unit line style.
 *
 * @param unit The unit system of the scalebar
 *
 * @since 200.7.0
 */
internal fun ScalebarProperties.computeAlternateUnitScalebarDivision(
    unit: UnitSystem,
): ScalebarDivision {
    val altUnit = if (unit == UnitSystem.Imperial) UnitSystem.Metric else UnitSystem.Imperial
    val altMapBaseLength =
        mapUnitsToDisplay.convertTo(altUnit.baseLinearUnit, scalebarLengthInMapUnits)
    val altClosestBaseLength =
        altUnit.closestDistanceWithoutGoingOver(altMapBaseLength, altUnit.baseLinearUnit)
    val altDisplayUnits = altUnit.linearUnitsForDistance(altClosestBaseLength)
    val altLengthInMapUnits = altUnit.baseLinearUnit.convertTo(altDisplayUnits, altClosestBaseLength)
    val displayFactor = scalebarLengthInMapUnits / displayLength
    val convertedDisplayFactor = mapUnitsToDisplay.convertTo(altDisplayUnits, displayFactor)
    val altDisplayLength = altLengthInMapUnits / convertedDisplayFactor
    val altUnitAbbr = altDisplayUnits.getAbbreviation()
    val label = "${altLengthInMapUnits.format()} $altUnitAbbr"
    return ScalebarDivision(
        xOffset = altDisplayLength,
        label = label
    )
}

/**
 * Computes the scalebar properties when geodetic calculations are used.
 *
 * @since 200.7.0
 */
private fun calculateGeodeticProperties(
    mapCenter: Point,
    spatialReference: SpatialReference,
    unitsPerDip: Double,
    maxLength: Double,
    units: UnitSystem
): Triple<Double, LinearUnit, Double> {
    val maxLengthPlanar = unitsPerDip * maxLength
    val polyline = Polyline(
        points = listOf(
            Point(
                x = mapCenter.x - (maxLengthPlanar * 0.5),
                y = mapCenter.y,
                spatialReference = spatialReference
            ), Point(
                x = mapCenter.x + (maxLengthPlanar * 0.5),
                y = mapCenter.y,
                spatialReference = spatialReference
            )
        ),
        spatialReference = spatialReference
    )
    val baseUnits = units.baseLinearUnit
    val maxLengthGeodetic = GeometryEngine.lengthGeodetic(
        polyline,
        baseUnits,
        GeodeticCurveType.Geodesic
    )
    val roundNumberDistance = units.closestDistanceWithoutGoingOver(
        maxLengthGeodetic,
        baseUnits
    )
    val planarToGeodeticFactor = maxLengthPlanar / maxLengthGeodetic
    val localDisplayLength = (roundNumberDistance * planarToGeodeticFactor) / unitsPerDip
    val localDisplayUnit = units.linearUnitsForDistance(roundNumberDistance)
    val localLineMapLength = baseUnits.convertTo(localDisplayUnit, roundNumberDistance)
    return Triple(localDisplayLength, localDisplayUnit, localLineMapLength)
}

/**
 * Computes the scalebar properties when planar calculations are used.
 *
 * @since 200.7.0
 */
private fun calculatePlanarProperties(
    spatialReference: SpatialReference,
    unitsPerDip: Double,
    maxLength: Double,
    units: UnitSystem
): Triple<Double, LinearUnit, Double> {
    val srUnit = spatialReference.unit as? LinearUnit ?: return Triple(0.0, LinearUnit.meters, 0.0)
    val baseUnits = units.baseLinearUnit
    val lenAvail = srUnit.convertTo(
        baseUnits,
        unitsPerDip * maxLength
    )
    val closestLen = units.closestDistanceWithoutGoingOver(
        lenAvail,
        baseUnits
    )
    val localDisplayLength = baseUnits.convertTo(
        srUnit,
        closestLen
    ) / unitsPerDip
    val localDisplayUnit = units.linearUnitsForDistance(closestLen)
    val localLineMapLength = baseUnits.convertTo(
        localDisplayUnit,
        closestLen
    )
    return Triple(localDisplayLength, localDisplayUnit, localLineMapLength)
}

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
