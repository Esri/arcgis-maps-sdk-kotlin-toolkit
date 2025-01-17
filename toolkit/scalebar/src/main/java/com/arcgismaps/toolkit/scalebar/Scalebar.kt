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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.scalebar.internal.ScalebarLabel
import com.arcgismaps.toolkit.scalebar.internal.lineWidth
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.internal.TextAlignment
import com.arcgismaps.toolkit.scalebar.internal.calculateSizeInDp
import com.arcgismaps.toolkit.scalebar.internal.drawHorizontalLine
import com.arcgismaps.toolkit.scalebar.internal.drawText
import com.arcgismaps.toolkit.scalebar.internal.drawTickMarks
import com.arcgismaps.toolkit.scalebar.internal.drawVerticalLine
import com.arcgismaps.toolkit.scalebar.internal.pixelAlignment
import com.arcgismaps.toolkit.scalebar.internal.scalebarHeight
import com.arcgismaps.toolkit.scalebar.internal.shadowOffset
import com.arcgismaps.toolkit.scalebar.internal.textOffset
import com.arcgismaps.toolkit.scalebar.internal.textSize
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
    maxWidth: Double, //  maximum screen width allotted to the scalebar
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
    LineScalebar(
        modifier = modifier,
        scaleValue = "1,000 km",
        maxWidth = maxWidth.toFloat(),
        colorScheme = colorScheme,
        shapes = shapes
    )
}

@Preview
@Composable
internal fun ScalebarPreview() {
    Scalebar(
        maxWidth = 200.0,
        spatialReference = null,
        unitsPerDip = 1.0,
        viewpoint = Viewpoint(0.0, 0.0, 0.0),
    )
}

/**
 * Displays a scalebar with single label and endpoint lines.
 *
 * @param modifier The modifier to apply to the layout.
 * @param scaleValue The scale value to display.
 * @param maxWidth The width of the scale bar.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 *
 * @since 200.7.0
 */
@Composable
internal fun LineScalebar(
    modifier: Modifier = Modifier.testTag("LineScalebar"),
    scaleValue: String,
    maxWidth: Float,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { textSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // left line
        drawVerticalLine(
            xPos = 0f,
            top = 0f,
            bottom = scalebarHeight,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // bottom line
        drawHorizontalLine(
            yPos = scalebarHeight,
            left = 0f,
            right = maxWidth,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLine(
            xPos = maxWidth,
            top = 0f,
            bottom = scalebarHeight,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // text label
        drawText(
            text = scaleValue,
            textMeasurer = textMeasurer,
            xPos = maxWidth / 2.0f,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            alignment = TextAlignment.CENTER
        )
    }
}

@Composable
internal fun GraduatedLineScalebar(
    modifier: Modifier = Modifier.testTag("GraduatedLineScalebar"),
    maxWidth: Float,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    tickMarks: List<ScalebarLabel>
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { textSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // draw tick marks
        drawTickMarks(
            tickMarks = tickMarks,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor,
            textMeasurer = textMeasurer,
            textColor = colorScheme.textColor,
            textShadowColor = colorScheme.textShadowColor
        )

        // bottom line
        drawHorizontalLine(
            yPos = scalebarHeight,
            left = 0f,
            right = maxWidth,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLine(
            xPos = maxWidth,
            top = 0f,
            bottom = scalebarHeight,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // draw last label
        drawText(
            text = tickMarks.last().text,
            textMeasurer = textMeasurer,
            xPos = tickMarks.last().xOffset.toFloat(),
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            alignment = TextAlignment.LEFT
        )
        drawText(
            text = " km",
            textMeasurer = textMeasurer,
            xPos = tickMarks.last().xOffset.toFloat(),
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            alignment = TextAlignment.RIGHT
        )
    }
}

/**
 * Displays bar scalebar with a single label.
 *
 * @param modifier The modifier to apply to the layout.
 * @param maxWidth The width of the scale bar.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 */
@Composable
internal fun BarScalebar(
    modifier: Modifier = Modifier.testTag("BarScalebar"),
    scaleValue: String,
    maxWidth: Float,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { textSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        drawRect(
            color = colorScheme.fillColor,
            topLeft = Offset(0f, 0f),
            size = Size(maxWidth, scalebarHeight)
        )
        drawRect(
            color = colorScheme.lineColor,
            topLeft = Offset(0f, 0f),
            size = Size(maxWidth, scalebarHeight),
            style = Stroke(width = lineWidth)
        )
        drawText(
            text = scaleValue,
            textMeasurer = textMeasurer,
            xPos = maxWidth / 2.0f,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            alignment = TextAlignment.CENTER
        )
    }
}
//@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun LineScaleBarPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        LineScalebar(
            modifier = Modifier,
            scaleValue = "1,000 km",
            maxWidth = 300f,
            colorScheme = ScalebarDefaults.colors(lineColor = Color.Red),
            shapes = ScalebarDefaults.shapes()
        )
    }
}

//@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun GraduatedScaleBarPreview() {
    val maxWidth = 500f
    val tickMarks = listOf(
        ScalebarLabel(0, 0.0, 0.0, "0"),
        ScalebarLabel(1, (maxWidth / 4.0), 0.0, "25"),
        ScalebarLabel(2, maxWidth / 2.0, 0.0, "50"),
        ScalebarLabel(3, (maxWidth / 4.0)* 3, 0.0, "75"),
        ScalebarLabel(4, maxWidth.toDouble(), 0.0, "100")
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        GraduatedLineScalebar(
            modifier = Modifier,
            maxWidth = maxWidth,
            colorScheme = ScalebarDefaults.colors(),
            shapes = ScalebarDefaults.shapes(),
            tickMarks = tickMarks
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun BarScaleBarPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        BarScalebar(
            modifier = Modifier,
            scaleValue = "1000 km",
            maxWidth = 300f,
            colorScheme = ScalebarDefaults.colors(),
            shapes = ScalebarDefaults.shapes()
        )
    }
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
 * Returns the display length of the Scalebar line.
 * // TODO: The computed length from this function needs to be passed to the updateScalebar function in the ScalebarViewModel.
 *
 * @return maxLength to be passed to updateScalebar fun in ScalebarViewModel
 * @since 200.7.0
 */
@Composable
private fun availableLineDisplayLength(
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
