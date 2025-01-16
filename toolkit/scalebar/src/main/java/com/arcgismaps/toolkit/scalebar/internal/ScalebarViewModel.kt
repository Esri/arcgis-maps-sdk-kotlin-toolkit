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
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography

internal class ScalebarViewModel(
    private val minScale: Double,
    private val style: ScalebarStyle,
    private val units: ScalebarUnits,
    private val labelTypography: LabelTypography,
    private val useGeodeticCalculations: Boolean
) : ViewModel() {

    private val labelPaddingX: Float = 4.0f

    private var lineMapLength: Double = 0.0
    private var displayUnit: LinearUnit? = null

    private val geodeticCurveType: GeodeticCurveType = GeodeticCurveType.Geodesic

    private var _isScaleBarUpdated: MutableState<Boolean> = mutableStateOf(false)
    val isScaleBarUpdated: State<Boolean> = _isScaleBarUpdated

    private var _displayLength: Double = 0.0
    val displayLength: Double
        get() = _displayLength

    private var _labels: MutableList<ScalebarLabel> = mutableListOf()
    val labels: List<ScalebarLabel>
        get() = _labels

    /**
     * Updates the labels for the Scalebar.
     *
     * @since 200.7.0
     */
    private fun updateLabels() {
        val localLabels = mutableListOf<ScalebarLabel>()
        val minSegmentTestString: String = if (lineMapLength >= 100) {
            lineMapLength.toInt().toString()
        } else {
            "9.9"
        }
        // TODO: Do we need to calculate this using UnitsPerDip?
        val minSegmentWidth = (minSegmentTestString.length * labelTypography.labelStyle.fontSize.value * 1.5) +
                (labelPaddingX * 2)

        val suggestedNumSegments = (displayLength / minSegmentWidth).toInt()

        // Cap segments at 4
        val maxNumSegments = minOf(suggestedNumSegments, 4)

        val numSegments = ScalebarUtils.numSegments(
            lineMapLength,
            maxNumSegments
        )

        val segmentScreenLength = displayLength / numSegments
        var currSegmentX = 0.0

        localLabels.add(
            ScalebarLabel(
                index = -1,
                xOffset = 0.0 ,
                yOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                text = "0" // TODO: localized this ?
            )
        )

        for (index in 0 until numSegments) {
            currSegmentX += segmentScreenLength
            val segmentMapLength = (segmentScreenLength * (index + 1) / displayLength) * lineMapLength

            val segmentText: String = if (index == numSegments - 1 && displayUnit != null) {
                val displayUnitAbbr = displayUnit?.getAbbreviation()
                "${segmentMapLength.toInt()} $displayUnitAbbr"
            } else {
                segmentMapLength.toString()
            }

            val label = ScalebarLabel(
                index = index,
                xOffset = currSegmentX,
                yOffset = labelTypography.labelStyle.fontSize.value / 2.0,
                text = segmentText
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
     * Updates the Scalebar with the new values.
     *
     * @since 200.7.0
     */
    fun updateScaleBar(
        spatialReference: SpatialReference?,
        viewpoint: Viewpoint?,
        unitsPerDip: Double?,
        maxLength: Double
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
        lineMapLength = localLineMapLength

        // update the labels
        updateLabels()
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
