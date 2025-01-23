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

import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils

/**
 * A Scalebar unit.
 *
 * @since 200.7.0
 */
public enum class ScalebarUnits {
    /**
     * Imperial units (feet, miles, etc)
     *
     * @since 200.7.0
     */
    IMPERIAL,

    /**
     * Metric units (meters, etc)
     *
     * @since 200.7.0
     */
    METRIC;

    /**
     * Determines an appropriate base linear unit for this scalebar unit.
     *
     * [ScalebarUnits.IMPERIAL] will return [LinearUnit.feet] as feet is the smallest linear
     *  unit that will be displayed.
     * [ScalebarUnits.METRIC] will return [LinearUnit.meters] as meter is the smallest linear
     *  unit that will be displayed.
     *
     * @return [LinearUnit.feet] for `IMPERIAL` and [LinearUnit.meters] for `METRIC`.
     *
     * @since 200.7.0
     */
    internal val baseLinearUnit: LinearUnit
        get() = if (this == IMPERIAL) LinearUnit.feet else LinearUnit.meters

    /**
     * Calculates a round number suitable for display.
     *
     *  @param distance The distance to calculate the round number for.
     *  @param units The units to calculate the round number for.
     *  @return The round number for a given distance and units.
     *
     *  @since 200.7.0
     */
    internal fun closestDistanceWithoutGoingOver(distance: Double, units: LinearUnit): Double {
        val magnitude = ScalebarUtils.magnitude(distance)
        val multiplier = ScalebarUtils.multiplier(distance)
        val roundNumber = multiplier * magnitude

        // Because feet and miles are not relationally multiples of 10 with
        // each other, we have to convert to miles if we are dealing in feet.
        if (units == LinearUnit.feet) {
            val displayUnits = linearUnitsForDistance(roundNumber)
            if (units != displayUnits) {
                val displayDistance = closestDistanceWithoutGoingOver(
                    units.convertTo(displayUnits, distance),
                    displayUnits
                )
                return displayUnits.convertTo(units, displayDistance)
            }
        }

        return roundNumber
    }

    /**
     * Returns a suitable display unit for the given distance.
     *
     * [ScalebarUnits.IMPERIAL] will return [LinearUnit.miles] if the given distance is greater
     *  than or equal to 1/2 mile, and [LinearUnit.feet] otherwise.
     * [ScalebarUnits.METRIC] will return [LinearUnit.kilometers] if the given distance is
     *  greater than or equal to 1 kilometer, and [LinearUnit.meters] otherwise.
     *
     * @param distance The distance to calculate the linear units for.
     * @return The linear units for a given distance.
     *
     * @since 200.7.0
     */
    internal fun linearUnitsForDistance(distance: Double): LinearUnit {
        return when (this) {
            IMPERIAL -> if (distance >= 2640) LinearUnit.miles else LinearUnit.feet
            METRIC -> if (distance >= 1000) LinearUnit.kilometers else LinearUnit.meters
        }
    }
}
