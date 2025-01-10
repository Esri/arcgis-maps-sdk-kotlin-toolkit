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

package com.arcgismaps.toolkit.scalebar

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

private const val pixelAlignment = 2.5f // Aligns the horizontal line edges
private const val lineWidth = 5f
private const val shadowOffset = 3f
private const val scalebarHeight = 20f // Height of the scalebar in pixels
private val textSize = 14.sp
private const val textOffset = 5f

/**
 * A composable UI component to display a line scalebar.
 * 
 * @since 200.7.0
 */
@Composable
internal fun LineScalebarImpl(
    modifier: Modifier = Modifier,
    scaleValue: String,
    width: Float,
    lineColor: Color = Color.Black,
    shadowColor: Color = Color.Unspecified,
    textColor: Color = Color.Black,
    textShadowColor: Color = Color.Unspecified
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val texSizeInPx = with(density) { textSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + texSizeInPx
    val totalWidth = width + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // left line
        drawVerticalLine(
            x = 0f,
            top = 0f,
            bottom = scalebarHeight,
            color = lineColor,
            shadowColor = shadowColor
        )

        // bottom line
        drawHorizontalLine(
            y = scalebarHeight,
            left = 0f,
            right = width,
            color = lineColor,
            shadowColor = shadowColor,
        )

        // right line
        drawVerticalLine(
            x = width,
            top = 0f,
            bottom = scalebarHeight,
            color = lineColor,
            shadowColor = shadowColor,
        )
        // text label
        drawText(
            text = scaleValue,
            textMeasurer = textMeasurer,
            barEnd = width,
            scalebarHeight = scalebarHeight,
            color = textColor,
            shadowColor = textShadowColor,
            alignment = TextAlignment.CENTER
        )
    }
}

@Composable
internal fun isMetric(): Boolean {
    // TODO implement the actual logic to determine the default ScalebarUnit
    // this is a placeholder implementation
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("isMetric", true)
}

/**
 * Calculates the size in dp based on the density of the device.
 *
 * @since 200.7.0
 */
private fun calculateSizeInDp(density: Density, value: Float) = with(density) {
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
 * The line will be of color [color] and the shadow will be of color [shadowColor].
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawVerticalLine(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color,
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
        color = color,
        start = Offset(x, top),
        end = Offset(x, bottom),
        strokeWidth = lineWidth,
    )
}

/**
 * Draws a horizontal line on the canvas with a shadow.
 * The line wil be of color [color] and the shadow will be of color [shadowColor].
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawHorizontalLine(
    y: Float,
    left: Float,
    right: Float,
    color: Color,
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
        color = color,
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

