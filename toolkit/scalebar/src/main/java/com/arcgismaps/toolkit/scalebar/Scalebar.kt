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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint

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
    spatialReference: SpatialReference?,
    unitsPerDips: Double?,
    viewpoint: Viewpoint?,
    modifier: Modifier = Modifier,
    uiProperties: UIProperties = UIProperties(),
    style: ScalebarStyle = ScalebarStyle.AlternatingBar,
    // TODO: determining the default ScalebarUnit is not tested
    units: ScalebarUnits = if (isMetric()) {
        ScalebarUnits.METRIC
    } else {
        ScalebarUnits.IMPERIAL
    },
    useGeodeticCalculations: Boolean = true, // `false` to compute scale without a geodesic curve
) {
}

@Preview
@Composable
internal fun ScalebarPreview() {
    Scalebar(
        maxWidth = 200.0,
        spatialReference = null,
        unitsPerDips = null,
        viewpoint = null
    )
}

/**
 * UI properties for the Scalebar.
 *
 * @property autoHideDelay wait time in seconds before the scalebar hides itself, -1 means never hide
 * @property minScale minimum scale to show the scalebar
 * @property barCornerRadius corner radius of the scalebar
 * @property fillColor fill color of the scalebar of the type bar
 * @property alternateFillColor alternate fill color of the scalebar of the type alternating bar
 * @property lineColor line color of the scalebar
 * @property shadowColor shadow color of the scalebar
 * @property shadowRadius shadow radius of the scalebar
 * @property textColor text color of the scalebar
 * @property textShadowColor text shadow color of the scalebar
 * @since 200.7.0
 */
public data class UIProperties(
    var autoHideDelay: Double = 1.75, // wait time in seconds before the scalebar hides itself
                                      // -1 means never hide
    var minScale: Double = 0.0, // minimum scale to show the scalebar
    var barCornerRadius: Double = 2.5, // corner radius of the scalebar
    var fillColor: Color = Color.Black,
    var alternateFillColor: Color = Color(0xFFEEEEEE), // light gray,
    var lineColor: Color = Color.White,
    var shadowColor: Color = Color.Black.copy(alpha = 0.65f),
    var shadowRadius: Double = 1.0,
    var textColor: Color = Color.Black,
    var textShadowColor: Color = Color.White
)

@Composable
private fun isMetric(): Boolean {
    // TODO implement the actual logic to determine the default ScalebarUnit
    // this is a placeholder implementation
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("isMetric", true)
}

/**
 * A Scalebar style.
 *
 * @since 200.7.0
 */
public enum class ScalebarStyle {
    /**
     * Displays a single unit with segmented bars of alternating fill color.
     *
     * @since 200.7.0
     */
    AlternatingBar,

    /**
     * Displays a single unit.
     *
     * @since 200.7.0
     */
    Bar,

    /**
     * Displays both metric and imperial units. The primary unit is displayed on top.
     *
     * @since 200.7.0
     */
    DualUnitLine,

    /**
     * Displays a single unit with a single bar.
     *
     * @since 200.7.0
     */
    GraduatedLine,

    /**
     * Displays a single unit with endpoint tick marks.
     *
     * @since 200.7.0
     */
    Line
}

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
    METRIC
}
