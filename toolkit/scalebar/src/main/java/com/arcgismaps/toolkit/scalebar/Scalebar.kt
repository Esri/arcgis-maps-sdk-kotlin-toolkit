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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.UnitSystem
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.toolkit.scalebar.internal.AlternatingBarScalebar
import com.arcgismaps.toolkit.scalebar.internal.BarScalebar
import com.arcgismaps.toolkit.scalebar.internal.DualUnitLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.GraduatedLineScalebar
import com.arcgismaps.toolkit.scalebar.internal.LineScalebar
import com.arcgismaps.toolkit.scalebar.internal.ScalebarDivision
import com.arcgismaps.toolkit.scalebar.internal.ScalebarProperties
import com.arcgismaps.toolkit.scalebar.internal.computeDivisions
import com.arcgismaps.toolkit.scalebar.internal.computeScalebarProperties
import com.arcgismaps.toolkit.scalebar.internal.labelXPadding
import com.arcgismaps.toolkit.scalebar.internal.lineWidth
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.theme.ScalebarColors
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.arcgismaps.toolkit.scalebar.theme.ScalebarShapes
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A composable UI component to display a Scalebar.
 * A Scalebar displays the representation of an accurate linear measurement on the map.
 * It provides a visual indication through which users can determine the size of features or
 * the distance between features on a map.
 *
 * The required parameters to display a Scalebar are the maximum width of the Scalebar in device independent pixels (dp),
 * the current unitsPerDip of the MapView, and the current [ViewpointType.CenterAndScale] of the MapView and the
 * [SpatialReference] of the MapView. The current value of unitsPerDip can be obtained from the Composable
 * MapView's onUnitsPerDipChanged callback. The [Viewpoint] can be obtained from the Composable MapView's
 * onViewpointChangedForCenterAndScale callback and the [SpatialReference] can be obtained from the Composable MapView's
 * onSpatialReferenceChanged callback.
 *
 * The Scalebar will be automatically updated when the Viewpoint changes.
 *
 * _Workflow example:_
 *
 *  ```
 *     var viewpoint: Viewpoint? by remember { mutableStateOf(null) }
 *     var unitsPerDip by remember { mutableDoubleStateOf(Double.NaN) }
 *     var spatialReference: SpatialReference? by remember { mutableStateOf(null) }
 *     // show composable MapView with a Scalebar
 *     Box(
 *         modifier = modifier.fillMaxSize()
 *     ) {
 *         MapView(
 *             modifier = Modifier.fillMaxSize(),
 *             arcGISMap = arcGISMap,
 *             onSpatialReferenceChanged = { spatialReference = it },
 *             onUnitsPerDipChanged = { unitsPerDip = it },
 *             onViewpointChangedForCenterAndScale = { viewpoint = it }
 *         )
 *         Scalebar(
 *             modifier = Modifier
 *                 .padding(25.dp)
 *                 .align(Alignment.BottomStart),
 *             maxWidth = 175.dp,
 *             unitsPerDip = unitsPerDip,
 *             viewpoint = viewpoint,
 *             spatialReference = spatialReference,
 *         )
 *     }
 *  ```
 *
 * @param maxWidth the maximum screen width allotted to the scalebar in dp.
 * @param unitsPerDip the number of map units per density-independent pixel (dp).
 * @param viewpoint the current viewpoint of the map.
 * @param spatialReference the spatial reference of the map.
 * @param modifier the modifier to apply to this layout.
 * @param autoHideDelay the duration to wait before hiding the scalebar, set to `Duration.INFINITE` to disable auto-hide.
 * @param minScale the minimum scale to show the scalebar, default is `0.0` which means the scalebar will always be visible.
 * @param useGeodeticCalculations `true` to compute scale using a geodesic curve, `false`otherwise, default is `true`.
 * @param style the style of the scalebar, default is [ScalebarStyle.AlternatingBar].
 * @param units the units for the scalebar, default is the default unit system based on the device's locale.
 * @param colorScheme the color scheme for the scalebar.
 * @param shapes the shapes for the scalebar.
 * @param labelTypography the typography for the scalebar labels.
 * @since 200.7.0
 */
@Composable
public fun Scalebar(
    maxWidth: Dp,
    unitsPerDip: Double,
    viewpoint: Viewpoint?,
    spatialReference: SpatialReference?,
    modifier: Modifier = Modifier,
    autoHideDelay: Duration = Duration.INFINITE,
    minScale: Double = 0.0,
    useGeodeticCalculations: Boolean = true,
    style: ScalebarStyle = ScalebarStyle.AlternatingBar,
    units: UnitSystem = rememberDefaultUnitSystem(),
    colorScheme: ScalebarColors = ScalebarDefaults.colors(),
    shapes: ScalebarShapes = ScalebarDefaults.shapes(),
    labelTypography: LabelTypography = ScalebarDefaults.typography()
) {
    val isScalebarVisible = remember(autoHideDelay) { mutableStateOf(true) }
    LaunchedEffect(viewpoint, autoHideDelay) {
        if (autoHideDelay > Duration.ZERO && autoHideDelay != Duration.INFINITE) {
            isScalebarVisible.value = true
            delay(autoHideDelay)
            isScalebarVisible.value = false
        }
    }
    val availableLineDisplayLength =
        measureAvailableLineDisplayLength(maxWidth.value.toDouble(), labelTypography, style)

    val scalebarProperties by remember(
        minScale,
        spatialReference,
        viewpoint,
        unitsPerDip,
        availableLineDisplayLength,
        useGeodeticCalculations,
        units
    ) {
        mutableStateOf(
            computeScalebarProperties(
                minScale = minScale,
                spatialReference = spatialReference,
                viewpoint = viewpoint,
                unitsPerDip = unitsPerDip,
                maxLength = availableLineDisplayLength,
                useGeodeticCalculations = useGeodeticCalculations,
                units = units
            )
        )
    }
    // prevent displaying the scalebar if the properties are invalid
    if (scalebarProperties == ScalebarProperties.NOT_INITIALIZED) {
        return
    }
    // Measure the minimum segment width required to display the labels without overlapping
    val minSegmentWidth = measureMinSegmentWidth(scalebarProperties.scalebarLengthInMapUnits, labelTypography)
    // update the label text and offsets
    val scalebarDivisions = scalebarProperties.computeDivisions(
        minSegmentWidth = minSegmentWidth,
        scalebarStyle = style,
        units = units
    )

    AnimatedVisibility(
        modifier = modifier,
        visible = isScalebarVisible.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Scalebar(
            displayLength = scalebarProperties.displayLength,
            labels = scalebarDivisions,
            scalebarStyle = style,
            colorScheme = colorScheme,
            shapes = shapes,
            labelTypography = labelTypography,
            modifier = modifier
        )
    }
}

@Composable
private fun Scalebar(
    displayLength: Double,
    labels: List<ScalebarDivision>,
    scalebarStyle: ScalebarStyle,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    labelTypography: LabelTypography,
    modifier: Modifier
) {
    if (labels.isEmpty()) {
        return
    }
    when (scalebarStyle) {
        ScalebarStyle.AlternatingBar -> AlternatingBarScalebar(
            modifier = modifier,
            displayLength = displayLength,
            scalebarDivisions = labels,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.Bar -> BarScalebar(
            modifier = modifier,
            displayLength = displayLength,
            label = labels[0].label,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.DualUnitLine -> DualUnitLineScalebar(
            modifier = modifier,
            primaryScalebarDivision = labels.first(),
            alternateScalebarDivision = labels.last(),
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.GraduatedLine -> GraduatedLineScalebar(
            modifier = modifier,
            displayLength = displayLength,
            tickMarks = labels,
            colorScheme = colorScheme,
            labelTypography = labelTypography,
            shapes = shapes
        )

        ScalebarStyle.Line -> LineScalebar(
            modifier = modifier,
            displayLength = displayLength,
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
    // Test the scalebar
    val viewPoint = Viewpoint(
        Point(-13046081.04434825, 4036489.208008117, SpatialReference.webMercator()),
        10000000.0
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) {
        Scalebar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(25.dp),
            maxWidth = 175.dp,
            unitsPerDip = 2645.833333330476,
            viewpoint = viewPoint,
            units = UnitSystem.Metric,
            spatialReference = SpatialReference.webMercator(),
            style = ScalebarStyle.Line,
            autoHideDelay = Duration.INFINITE
        )
    }
}

@Composable
private fun rememberDefaultUnitSystem(): UnitSystem {
    val locale = Locale.current
    val unitSystem = when (locale.platformLocale.country) {
        "US", "LR", "MM" -> UnitSystem.Imperial// United States, Liberia, Myanmar
        else -> UnitSystem.Metric
    }
    return remember(locale) { unitSystem }
}

/**
 * Returns the display length in device independent pixels of the Scalebar line.
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
            maxWidth - (lineWidth.value / 2.0f)
        }
    }
}

/**
 * Returns the minimum segment width in device independent pixels required to display the labels
 * without overlapping.
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
