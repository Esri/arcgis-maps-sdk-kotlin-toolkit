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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

internal class ScalebarViewModel(
    private val minScale: Double,
    private val style: ScalebarStyle,
    private val units: ScalebarUnits,
    private val labelTypography: LabelTypography,
    private val useGeodeticCalculations: Boolean
) : ViewModel() {

    private var displayUnit: LinearUnit? = null

    private val geodeticCurveType: GeodeticCurveType = GeodeticCurveType.Geodesic

    private var _isScaleBarUpdated: MutableState<Boolean> = mutableStateOf(false)
    val isScaleBarUpdated: State<Boolean> = _isScaleBarUpdated

    private var _isUpdateLabels: MutableState<Boolean> = mutableStateOf(false)
    val isUpdateLabels: State<Boolean> = _isUpdateLabels

    private var _displayLength: Double = 0.0
    val displayLength: Double
        get() = _displayLength

    private var _lineMapLength: Double = 0.0
    val lineMapLength: Double
        get() = _lineMapLength

    private var _labels: MutableList<ScalebarDivision> = mutableListOf()
    val labels: List<ScalebarDivision>
        get() = _labels

    internal val alternateUnit: ScalebarDivision
        get() {
            val displayUnit = displayUnit ?: return ScalebarDivision(-1, 0.0, 0.0, "")
            val altUnit = if (units == ScalebarUnits.IMPERIAL) ScalebarUnits.METRIC else ScalebarUnits.IMPERIAL
            val altMapBaseLength = displayUnit.convertTo(altUnit.baseLinearUnit, lineMapLength)
            val altClosestBaseLength = altUnit.closestDistanceWithoutGoingOver(altMapBaseLength, altUnit.baseLinearUnit)
            val altDisplayUnits = altUnit.linearUnitsForDistance(altClosestBaseLength)
            val altMapLength = altUnit.baseLinearUnit.convertTo(altDisplayUnits, altClosestBaseLength)
            val displayFactor = lineMapLength / displayLength
            val convertedDisplayFactor = displayUnit.convertTo(altDisplayUnits, displayFactor)
            val altScreenLength = altMapLength / convertedDisplayFactor

            val altUnitAbbr = altDisplayUnits.getAbbreviation()
            val label = "${altMapLength.format()} $altUnitAbbr"

            return ScalebarDivision(
                index = -2,
                xOffset = altScreenLength,
                labelYOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                label = label
            )
        }

    /**
     * Updates the labels for the Scalebar.
     *
     * @since 200.7.0
     */
    internal fun updateLabels(minSegmentWidth: Double) {
        val suggestedNumSegments = (displayLength / minSegmentWidth).toInt()

        // Cap segments at 4
        val maxNumSegments = minOf(suggestedNumSegments, 4)

        val numSegments = ScalebarUtils.numSegments(
            lineMapLength,
            maxNumSegments
        )

        val segmentScreenLength = displayLength / numSegments
        var currSegmentX = 0.0
        val localLabels = mutableListOf<ScalebarDivision>()

        localLabels.add(
            ScalebarDivision(
                index = -1,
                xOffset = 0.0 ,
                labelYOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                label = "0"
            )
        )

        for (index in 0 until numSegments) {
            currSegmentX += segmentScreenLength
            val segmentMapLength: Double = (segmentScreenLength * (index + 1) / displayLength) * lineMapLength

            val segmentText: String = if (index == numSegments - 1 && displayUnit != null) {
                val displayUnitAbbr = displayUnit?.getAbbreviation()
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

        if (style == ScalebarStyle.Bar || style == ScalebarStyle.Line) {
            localLabels.lastOrNull()?.let {
                _labels = mutableListOf(it)
            }
        } else {
            _labels = localLabels
        }
        _isScaleBarUpdated.value = true
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
    ) {
        if (spatialReference == null || unitsPerDip == null || viewpoint == null) {
            return
        }

        if (minScale > 0 && viewpoint.targetScale >= minScale || unitsPerDip.isNaN()) {
            return
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
            val srUnit = spatialReference.unit as? LinearUnit ?: return
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
            return
        }

        // update the scalebar with the new values
        _displayLength = localDisplayLength
        displayUnit = localDisplayUnit
        _lineMapLength = localLineMapLength

        // update the labels
        _isUpdateLabels.value = true
    }
}

internal class ScalebarViewModelFactory(
    private val minScale: Double,
    private val style: ScalebarStyle,
    private val units: ScalebarUnits,
    private val labelTypography: LabelTypography,
    private val useGeodeticCalculations: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScalebarViewModel::class.java)) {
            return ScalebarViewModel(minScale, style, units, labelTypography, useGeodeticCalculations) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
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
