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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

internal const val pixelAlignment = 2.5f // Aligns the horizontal line edges
internal const val lineWidth = 5f
internal const val shadowOffset = 3f
internal const val scalebarHeight = 20f // Height of the scalebar in pixels
internal val textSize = 14.sp
internal const val textOffset = 5f

/**
 * Calculates the size in dp based on the density of the device.
 *
 * @since 200.7.0
 */
internal fun calculateSizeInDp(density: Density, value: Float) = with(density) {
    value.toDp()
}

/**
 * Used to align the text relative to a point in the scalebar.
 *
 * @since 200.7.0
 */
internal enum class TextAlignment {
    /**
     * Left align the text relative to a point.
     * @since 200.7.0
     */
    LEFT,

    /**
     * Center align the text relative to a point.
     * @since 200.7.0
     */
    CENTER,

    /**
     * Right aligns the text relative to a point.
     * @since 200.7.0
     */
    RIGHT,
}

/**
 * Draws a vertical line on the canvas at the [xPos] of color [color] with a shadow of color [shadowColor].
 * The line height will be determined by [top] and [bottom] positions.
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawVerticalLine(
    xPos: Float,
    top: Float,
    bottom: Float,
    color: Color,
    shadowColor: Color,
) {
    // draw shadow
    drawLine(
        color = shadowColor,
        start = Offset(xPos + shadowOffset, top),
        end = Offset(xPos + shadowOffset, bottom),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = color,
        start = Offset(xPos, top),
        end = Offset(xPos, bottom),
        strokeWidth = lineWidth,
    )
}

/**
 * Draws a horizontal line on the canvas at the [yPos] with a color [color] and a shadow of color [shadowColor].
 * The line width will be determined by [left] and [right] positions.
 *
 * @since 200.7.0
 */
internal fun DrawScope.drawHorizontalLine(
    yPos: Float,
    left: Float,
    right: Float,
    color: Color,
    shadowColor: Color,
) {
    // draw shadow
    drawLine(
        color = shadowColor,
        start = Offset((left - pixelAlignment) + shadowOffset, yPos + shadowOffset),
        end = Offset((right + pixelAlignment) + shadowOffset, yPos + shadowOffset),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = color,
        start = Offset(left - pixelAlignment, yPos),
        end = Offset(right + pixelAlignment, yPos),
        strokeWidth = lineWidth,
    )
}

/**
 * Draws the text on the canvas with a shadow.
 * This method adds blank space of size [textOffset] between the scaleBar and the text.
 *
 * @param text The text to be drawn.
 * @param textMeasurer The [TextMeasurer] to measure the text.
 * @param xPos The location where the text should be drawn.
 * @param color The color of the text.
 * @param shadowColor The color of the text shadow.
 * @param alignment The alignment of text relative to [xPos].
 * @since 200.7.0
 */
internal fun DrawScope.drawText(
    text: String,
    textMeasurer: TextMeasurer,
    xPos: Float,
    color: Color = Color.Black,
    shadowColor: Color = Color.White,
    alignment: TextAlignment = TextAlignment.CENTER
) {
    val measuredText = textMeasurer.measure(
        text = text,
        style = TextStyle(fontSize = textSize)
    )
    val alignedXPos = when (alignment) {
        TextAlignment.LEFT -> xPos - measuredText.size.width + pixelAlignment
        TextAlignment.CENTER -> xPos - (measuredText.size.width / 2)
        TextAlignment.RIGHT -> xPos - pixelAlignment
    }
    val yPos = scalebarHeight + textOffset
    drawText(
        measuredText,
        color = color,
        topLeft = Offset(alignedXPos, yPos),
        shadow = Shadow(color = shadowColor, offset = Offset(1f, 1f))
    )
}

/**
 * Draws the tick marks on the canvas of height [tickHeight] with a color [color] and shadow of color [shadowColor].
 *
 * The text of the tick marks will be drawn with a color [textColor] and shadow of color [textShadowColor]. The tickmark
 * position will be determined by [ScalebarLabel.xOffset].
 */
internal fun DrawScope.drawTickMarks(
    tickMarks: List<ScalebarLabel>,
    tickHeight: Float = 10f,
    color: Color,
    shadowColor: Color,
    textMeasurer: TextMeasurer,
    textColor: Color,
    textShadowColor: Color
) {

    for (i in 0 until tickMarks.size - 1) {
        drawVerticalLine(
            xPos = tickMarks[i].xOffset.toFloat(),
            top = if (i == 0) 0f else scalebarHeight - tickHeight,
            bottom = scalebarHeight,
            color = color,
            shadowColor = shadowColor
        )
        drawText(
            text = tickMarks[i].text,
            textMeasurer = textMeasurer,
            xPos = tickMarks[i].xOffset.toFloat(),
            color = textColor,
            shadowColor = textShadowColor,
            alignment = if (i == 0) TextAlignment.RIGHT else TextAlignment.CENTER
        )
    }
}


