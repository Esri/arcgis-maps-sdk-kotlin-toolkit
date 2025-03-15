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

import androidx.compose.ui.unit.Density
import com.arcgismaps.UnitSystem
import com.arcgismaps.geometry.LinearUnit
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utility class for the Scalebar.
 *
 * @since 200.7.0
 */
internal object ScalebarUtils {
    /**
     * Multipliers for rounding numbers
     * This table must begin with 1 and end with 10.
     *
     * @since 200.7.0
     */
    private val roundNumberMultipliers = listOf(
        1.0, 1.2, 1.25, 1.5, 1.75, 2.0, 2.4, 2.5, 3.0, 3.75, 4.0, 5.0, 6.0, 7.5, 8.0, 9.0, 10.0
    )
    /**
     * Calculates the highest power of 10 that fits into the given distance.
     *
     * For example:
     * A distance of 25 will return 10 as 10 is the highest power of 10 that will fit into 25.
     * A distance of 550 will return 100 as 100 is the highest power of 10 that will fit into 550.
     * A distance of 2,222 will return 1000 as 1000 is the highest power of 10 that will fit into 2,222.
     *
     * @param distance The distance to calculate the magnitude for.
     * @return The highest power of 10 that fits into the given distance.
     *
     * @since 200.7.0
     */
    internal fun magnitude(distance: Double): Double {
        return 10.0.pow(floor(log10(distance)))
    }

    /**
     * Returns the multiplier for a given distance.
     *
     *
     * @param distance The distance to calculate the multiplier for.
     * @return The multiplier for a given distance.
     *
     * @since 200.7.0
     */
    internal fun multiplier(distance: Double): Double {
        val residual = distance / magnitude(distance)
        return roundNumberMultipliers.lastOrNull { it <= residual } ?: 0.0
    }

    /**
     * Returns the segment options for a given multiplier.
     *
     * @param multiplier The multiplier to calculate the segment options for.
     * @return The segment options for a given multiplier.
     *
     * @since 200.7.0
     */
    private fun segmentOptions(multiplier: Double): List<Int> {
        return when (multiplier) {
            1.0 -> listOf(1, 2, 4, 5)
            1.2 -> listOf(1, 2, 3, 4)
            1.25 -> listOf(1, 2)
            1.5 -> listOf(1, 2, 3, 5)
            1.75 -> listOf(1, 2)
            2.0 -> listOf(1, 2, 4, 5)
            2.4 -> listOf(1, 2, 3)
            2.5 -> listOf(1, 2, 5)
            3.0 -> listOf(1, 2, 3)
            3.75 -> listOf(1, 3)
            4.0 -> listOf(1, 2, 4)
            5.0 -> listOf(1, 2, 5)
            6.0 -> listOf(1, 2, 3)
            7.5 -> listOf(1, 2)
            8.0 -> listOf(1, 2, 4)
            9.0 -> listOf(1, 2, 3)
            10.0 -> listOf(1, 2, 5)
            else -> listOf(1)
        }
    }

    /**
     * Returns the best number of segments so that we get relatively round numbers when the
     * distance is divided up.
     *
     * @param distance The distance to calculate the number of segments for.
     * @param maxNumSegments The maximum number of segments.
     * @return The number of segments for a given distance and maximum number of segments.
     *
     * @since 200.7.0
     */
    internal fun numSegments(distance: Double, maxNumSegments: Int): Int {
        val multiplier = multiplier(distance)
        val options = segmentOptions(multiplier)
        return options.lastOrNull { it <= maxNumSegments } ?: 1
    }

    /**
     * Formats a double to a string.
     * If the double has no decimal part or the first two decimal places are 0, then it will be displayed as an int.
     * If the second decimal place is zero, it will display the number with one decimal place.
     * Otherwise, it will truncate the number and be displayed with two decimal places.
     *
     * @since 200.7.0
     */
    fun Double.format(): String {
        return when {
            (this - this.toInt()) < 1e-6 -> this.toInt().toString()
            (this * 10) % 1.0 == 0.0 -> "%.1f".format(this)
            else -> "%.2f".format(this)
        }
    }

    /**
     * Calculates the size in pixels based on the density of the device.
     *
     * @since 200.7.0
     */
    fun Double.toPx(density: Density): Float = (this * density.density).toFloat()

    /**
     * Determines an appropriate base linear unit for this [UnitSystem].
     *
     * [UnitSystem.Imperial] will return [LinearUnit.feet] as feet is the smallest linear
     *  unit that will be displayed.
     * [UnitSystem.Metric] will return [LinearUnit.meters] as meter is the smallest linear
     *  unit that will be displayed.
     *
     * @return [LinearUnit.feet] for `IMPERIAL` and [LinearUnit.meters] for `METRIC`.
     *
     * @since 200.7.0
     */
    internal val UnitSystem.baseLinearUnit: LinearUnit
        get() = if (this == UnitSystem.Imperial) LinearUnit.feet else LinearUnit.meters

    /**
     * Calculates a round number suitable for display.
     *
     *  @param distance The distance to calculate the round number for.
     *  @param units The units to calculate the round number for.
     *  @return The round number for a given distance and units.
     *
     *  @since 200.7.0
     */
    internal fun UnitSystem.closestDistanceWithoutGoingOver(distance: Double, units: LinearUnit): Double {
        val magnitude = magnitude(distance)
        val multiplier = multiplier(distance)
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
     * [UnitSystem.Imperial] will return [LinearUnit.miles] if the given distance is greater
     *  than or equal to 1/2 mile, and [LinearUnit.feet] otherwise.
     * [UnitSystem.Metric] will return [LinearUnit.kilometers] if the given distance is
     *  greater than or equal to 1 kilometer, and [LinearUnit.meters] otherwise.
     *
     * @param distance The distance to calculate the linear units for.
     * @return The linear units for a given distance.
     *
     * @since 200.7.0
     */
    internal fun UnitSystem.linearUnitsForDistance(distance: Double): LinearUnit {
        return when (this) {
            UnitSystem.Imperial -> if (distance >= 2640) LinearUnit.miles else LinearUnit.feet
            UnitSystem.Metric -> if (distance >= 1000) LinearUnit.kilometers else LinearUnit.meters
        }
    }
}
