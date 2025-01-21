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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.toolkit.scalebar.theme.ScalebarColors
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.arcgismaps.toolkit.scalebar.theme.ScalebarShapes

private const val pixelAlignment = 2.5f // Aligns the horizontal line edges
internal const val lineWidth = 5f
private const val shadowOffset = 3f
private const val scalebarHeight = 20f // Height of the scalebar in pixels
private val textSize = 14.sp
private const val textOffset = 5f
internal const val labelXPadding = 4f // padding between scalebar labels.

/**
 * Displays a scalebar with single label and endpoint lines.
 *
 * @param modifier The modifier to apply to the layout.
 * @param label The scale value to display.
 * @param maxWidth The width of the scalebar in pixels.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 *
 * @since 200.7.0
 */
@Composable
internal fun LineScalebar(
    modifier: Modifier = Modifier.testTag("LineScalebar"),
    label: String,
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
        drawVerticalLineAndShadow(
            x = 0f,
            top = 0f,
            bottom = scalebarHeight,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // bottom line
        drawHorizontalLineAndShadow(
            y = scalebarHeight,
            left = 0f,
            right = maxWidth,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLineAndShadow(
            x = maxWidth,
            top = 0f,
            bottom = scalebarHeight,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // text label
        drawText(
            text = label,
            textMeasurer = textMeasurer,
            barEnd = maxWidth,
            scalebarHeight = scalebarHeight,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            alignment = TextAlignment.CENTER
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun LineScaleBarPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(4.dp), contentAlignment = Alignment.BottomStart) {
        LineScalebar(
            modifier = Modifier,
            label = "1,000 km",
            maxWidth = 300f,
            colorScheme = ScalebarDefaults.colors(lineColor = Color.Red),
            shapes = ScalebarDefaults.shapes()
        )
    }
}

/**
 * Calculates the size in dp based on the density of the device.
 *
 * @since 200.7.0
 */
internal fun calculateSizeInDp(density: Density, value: Float) = with(density) {
    value.toDp()
}

/**
 * Used to align the text relative to the scalebar.
 *
 * @since 200.7.0
 */
internal enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Draws a vertical line on the canvas with a shadow.
 * The line will be of color [lineColor] and the shadow will be of color [shadowColor].
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawVerticalLineAndShadow(
    x: Float,
    top: Float,
    bottom: Float,
    lineColor: Color,
    shadowColor: Color,
) {
    // draw shadow
    drawLine(
        color = shadowColor,
        start = Offset(x + shadowOffset, top),
        end = Offset(x + shadowOffset, bottom),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = lineColor,
        start = Offset(x, top),
        end = Offset(x, bottom),
        strokeWidth = lineWidth,
    )
}

/**
 * Draws a horizontal line on the canvas with a shadow.
 * The line will be of color [lineColor] and the shadow will be of color [shadowColor].
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawHorizontalLineAndShadow(
    y: Float,
    left: Float,
    right: Float,
    lineColor: Color,
    shadowColor: Color,
) {
    // draw shadow
    drawLine(
        color = shadowColor,
        start = Offset((left - pixelAlignment) + shadowOffset, y + shadowOffset),
        end = Offset((right + pixelAlignment) + shadowOffset, y + shadowOffset),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = lineColor,
        start = Offset(left - pixelAlignment, y),
        end = Offset(right + pixelAlignment, y),
        strokeWidth = lineWidth,
    )
}

/**
 * Draws the text on the canvas with a shadow.
 * This method adds blank space of size [textOffset] between the scaleBar and the text.
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawText(
    text: String,
    textMeasurer: TextMeasurer,
    barStart: Float = 0f,
    barEnd: Float,
    scalebarHeight: Float,
    color: Color = Color.Black,
    shadowColor: Color = Color.White,
    alignment: TextAlignment = TextAlignment.CENTER
) {
    val measuredText = textMeasurer.measure(
        text = text,
        style = TextStyle(fontSize = textSize)
    )
    val xPos = when (alignment) {
        TextAlignment.LEFT -> barStart - (measuredText.size.width / 2)
        TextAlignment.CENTER -> (barEnd - measuredText.size.width) / 2
        TextAlignment.RIGHT -> barEnd - (measuredText.size.width / 2)
    }
    val yPos = scalebarHeight + textOffset
    drawText(
        measuredText,
        color = color,
        topLeft = Offset(xPos, yPos),
        shadow = Shadow(color = shadowColor, offset = Offset(1f, 1f))
    )
}
