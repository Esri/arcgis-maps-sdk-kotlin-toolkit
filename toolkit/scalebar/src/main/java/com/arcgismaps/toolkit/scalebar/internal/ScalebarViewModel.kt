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

import androidx.lifecycle.ViewModel
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.Scalebar
import com.arcgismaps.toolkit.scalebar.ScalebarStyle
import com.arcgismaps.toolkit.scalebar.ScalebarUnits

internal class ScalebarViewModel : ViewModel() {

    private var lineMapLength: Double = 0.0


    /**
     *
     *
     * @since 200.7.0
     */
    fun updateLabels(displayLength: Double, displayUnit: LinearUnit?, style: ScalebarStyle) {
        val lineDisplayLength = displayLength

        val minSegmentTestString: String = if (lineMapLength >= 100) {
            lineMapLength.toInt().toString()
        } else {
            "9.9"
        }

        val minSegmentWidth = (minSegmentTestString.length * Scalebar.font.uiFont.size * 1.5) +
                (Scalebar.labelXPad * 2)

        val suggestedNumSegments = (lineDisplayLength / minSegmentWidth).toInt()

        val maxNumSegments = min(suggestedNumSegments, 4)

        val numSegments = ScalebarUnits.numSegments(
            lineMapLength,
            maxNumSegments
        )

        val segmentScreenLength = lineDisplayLength / numSegments
        var currSegmentX = 0.0
        val labels = mutableListOf<ScalebarLabel>()

        labels.add(
            ScalebarLabel(
                index = -1,
                xOffset = 0.0,
                text = "0"
            )
        )

        for (index in 0 until numSegments) {
            currSegmentX += segmentScreenLength
            val segmentMapLength = (segmentScreenLength * (index + 1) / lineDisplayLength) * lineMapLength

            val segmentText: String = if (index == numSegments - 1 && displayUnit != null) {
                val measurement = Measurement(
                    value = segmentMapLength,
                    linearUnit = displayUnit
                )
                measurement.formatted(MeasurementFormatter.Style.SCALE_MEASUREMENT)
            } else {
                segmentMapLength.toString()
            }

            val label = ScalebarLabel(
                index = index,
                xOffset = currSegmentX,
                text = segmentText
            )
            labels.add(label)
        }

        if (style == ScalebarStyle.BAR || style == ScalebarStyle.LINE) {
            labels.lastOrNull()?.let {
                this.labels = listOf(it)
            }
        } else {
            this.labels = labels
        }
    }

    /**
     * Updates the Scalebar with the new values.
     *
     * @since 200.7.0
     */
    fun updateScaleBar(
        spatialReference: SpatialReference?,
        unitsPerDip: Double?,
        viewpoint: Viewpoint?,
        minScale: Double,
        units: ScalebarUnits,
        availableLineDisplayLength: Double,
        useGeodeticCalculations: Boolean
    ) {
        if (spatialReference == null || unitsPerDip == null || viewpoint == null) {
            return
        }

        if (minScale > 0 && viewpoint.targetScale >= minScale || unitsPerDip.isNaN()) {
            return
        }

        val mapCenter = viewpoint.targetGeometry.extent.center
        // TODO: determine the maximum length of the line that can be displayed add implementation
        val maxLength = availableLineDisplayLength

        val lineMapLength: Double
        val displayUnit: LinearUnit
        val displayLength: Double

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
                GeodeticCurveType.Geodesic
            )
            val roundNumberDistance = units.closestDistanceWithoutGoingOver(
                maxLengthGeodetic,
                baseUnits
            )
            val planarToGeodeticFactor = maxLengthPlanar / maxLengthGeodetic
            displayLength = (roundNumberDistance * planarToGeodeticFactor) / unitsPerDip
            displayUnit = units.linearUnitsForDistance(roundNumberDistance)
            lineMapLength = baseUnits.convertTo(displayUnit, roundNumberDistance)
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
            displayLength = baseUnits.convertTo(
                srUnit,
                closestLen
            ) / unitsPerDip
            displayUnit = units.linearUnitsForDistance(closestLen)
            lineMapLength = baseUnits.convertTo(
                displayUnit,
                closestLen
            )
        }

        if (!displayLength.isFinite() || displayLength.isNaN()) {
            return
        }

        // TODO: update the scalebar with the new values
//    this.displayLength = displayLength
//    this.displayUnit = displayUnit
//    this.lineMapLength = lineMapLength

        // TODO: handle the case when the initial scale was calculated
//    initialScaleWasCalculated = true
        // TODO: update the labels
//    updateLabels()

    }

    // TODO: implement the actual logic to determine the maximum length of the line that can be displayed
    private fun availableLineDisplayLength(): Double {
        return 0.0
    }

}