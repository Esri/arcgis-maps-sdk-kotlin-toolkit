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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp

private const val pixelAlignment = 2.5f // Aligns the line edges
private const val lineWidth = 5f
private const val shadowOffset = 3f
private val textSize = 14.sp

internal enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

internal fun DrawScope.drawVerticalLine(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color,
    shadowColor: Color,
) {
    // shadow
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
    val yPos = scalebarHeight + 5f
    drawText(
        measuredText,
        color = color,
        topLeft = Offset(xPos, yPos),
        shadow = Shadow(color = shadowColor, offset = Offset(1f, 1f))
    )
}

