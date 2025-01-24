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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.theme.ScalebarColors
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.arcgismaps.toolkit.scalebar.theme.ScalebarShapes

private const val pixelAlignment = 2.5f // Aligns the horizontal line edges
internal const val lineWidth = 5f
private const val shadowOffset = 3f
private const val scalebarHeight = 20f // Height of the scalebar in pixels
private const val textOffset = 5f
internal const val labelXPadding = 4f // padding between scalebar labels.

/**
 * Displays a scalebar with single label and endpoint lines.
 *
 * @param modifier The modifier to apply to the layout.
 * @param maxWidth The width of the scalebar in pixels.
 * @param label The scale value to display.
 * @param colorScheme The color scheme to use.
 * @param labelTypography The typography to use for the label.
 * @param shapes The shape properties to use.
 * @since 200.7.0
 */
@Composable
internal fun LineScalebar(
    modifier: Modifier = Modifier.testTag("LineScalebar"),
    maxWidth: Float,
    label: String,
    colorScheme: ScalebarColors,
    labelTypography: LabelTypography,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { labelTypography.labelStyle.fontSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // left line
        drawVerticalLineAndShadow(
            xPos = 0f,
            top = 0f,
            bottom = scalebarHeight,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // bottom line
        drawHorizontalLineAndShadow(
            yPos = scalebarHeight,
            left = 0f,
            right = maxWidth,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLineAndShadow(
            xPos = maxWidth,
            top = 0f,
            bottom = scalebarHeight,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // text label
        drawText(
            text = label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = maxWidth / 2,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius.toFloat(),
            alignment = TextAlignment.CENTER
        )
    }
}

/**
 * Displays bar scalebar with a single label.
 *
 * @param modifier The modifier to apply to the layout.
 * @param maxWidth The width of the scale bar.
 * @param label The scale value to display.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 * @param labelTypography The typography to use for the label.
 * @since 200.7.0
 */
@Composable
internal fun BarScalebar(
    modifier: Modifier = Modifier.testTag("BarScalebar"),
    maxWidth: Float,
    label: String,
    colorScheme: ScalebarColors,
    topLeftPoint : Offset = Offset(0f, 0f),
    shapes: ScalebarShapes,
    labelTypography: LabelTypography
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { labelTypography.labelStyle.fontSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // draws the rectangle's shadow
        drawRoundRect(
            color = colorScheme.shadowColor,
            topLeft = Offset(topLeftPoint.x + shadowOffset, topLeftPoint.y + shadowOffset),
            size = Size(maxWidth, scalebarHeight),
            cornerRadius = CornerRadius(shapes.barCornerRadius.toFloat()),
            style = Stroke(width = lineWidth)
        )
        
        // Draws the rectangle's fill color
        drawRoundRect(
            color = colorScheme.fillColor,
            topLeft = topLeftPoint,
            cornerRadius = CornerRadius(shapes.barCornerRadius.toFloat()),
            size = Size(maxWidth, scalebarHeight),

        )
        // draws the rectangle's border
        drawRoundRect(
            color = colorScheme.lineColor,
            topLeft = topLeftPoint,
            size = Size(maxWidth, scalebarHeight),
            cornerRadius = CornerRadius(shapes.barCornerRadius.toFloat()),
            style = Stroke(width = lineWidth)
        )

        drawText(
            text = label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = maxWidth / 2.0f,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius.toFloat(),
            alignment = TextAlignment.CENTER
        )
    }
}

/**
 * Displays a graduated scalebar with multiple labels and tick marks.
 *
 * @param modifier The modifier to apply to the layout.
 * @param maxWidth The width of the scale bar.
 * @param tickMarks The list of tick marks to display.
 * @param colorScheme The color scheme to use.
 * @param labelTypography The typography to use for the label.
 * @param shapes The shape properties to use.
 * @since 200.7.0
 */
@Composable
internal fun GraduatedLineScalebar(
    modifier: Modifier = Modifier.testTag("GraduatedLineScalebar"),
    maxWidth: Float,
    tickMarks: List<ScalebarDivision>,
    colorScheme: ScalebarColors,
    labelTypography: LabelTypography,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textSizeInPx = with(density) { labelTypography.labelStyle.fontSize.toPx() }

    val totalHeight = scalebarHeight + shadowOffset + textOffset + textSizeInPx
    val totalWidth = maxWidth + shadowOffset + pixelAlignment

    Canvas(
        modifier = modifier
            .width(calculateSizeInDp(density, totalWidth))
            .height(calculateSizeInDp(density, totalHeight))
    ) {
        // draw tick marks
        drawTickMarksWithLabels(
            tickMarks = tickMarks,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            textColor = colorScheme.textColor,
            textShadowColor = colorScheme.textShadowColor,
            textShadowBlurRadius = shapes.textShadowBlurRadius.toFloat()
        )

        // bottom line
        drawHorizontalLineAndShadow(
            yPos = scalebarHeight,
            left = 0f,
            right = maxWidth,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLineAndShadow(
            xPos = maxWidth,
            top = 0f,
            bottom = scalebarHeight,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // draw last label
        drawText(
            text = tickMarks.last().label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = tickMarks.last().xOffset.toFloat(),
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius.toFloat(),
            alignment = TextAlignment.CENTER
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun LineScaleBarPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        LineScalebar(
            modifier = Modifier,
            maxWidth = 300f,
            label = "1,000 km",
            colorScheme = ScalebarDefaults.colors(),
            labelTypography = ScalebarDefaults.typography(),
            shapes = ScalebarDefaults.shapes()
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
            maxWidth = 300f,
            label = "1000 km",
            colorScheme = ScalebarDefaults.colors(shadowColor = Color.Red, textShadowColor = Color.Red),
            shapes = ScalebarDefaults.shapes(barCornerRadius = 4.0, textShadowBlurRadius = 2.0),
            labelTypography = ScalebarDefaults.typography()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun GraduatedLineScaleBarPreview() {
    val maxWidth = 500f
    val tickMarks = listOf(
        ScalebarDivision(0, 0.0, 0.0, "0"),
        ScalebarDivision(1, (maxWidth / 4.0), 0.0, "25"),
        ScalebarDivision(2, maxWidth / 2.0, 0.0, "50"),
        ScalebarDivision(3, (maxWidth / 4.0)* 3, 0.0, "75"),
        ScalebarDivision(4, maxWidth.toDouble(), 0.0, "100 km")
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
            tickMarks = tickMarks,
            labelTypography = ScalebarDefaults.typography(),
            shapes = ScalebarDefaults.shapes()
        )
    }
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
 * Used to align the text relative to an anchor point in the scalebar.
 *
 * @since 200.7.0
 */
private enum class TextAlignment {
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
 * Draws a vertical line of [color] on the canvas at the [xPos] with a shadow of color [shadowColor].
 * The line height will be determined by [top] and [bottom] positions.
 *
 * @since 200.7.0
 */
private fun DrawScope.drawVerticalLineAndShadow(
    xPos: Float,
    top: Float,
    bottom: Float,
    lineColor: Color,
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
            color = lineColor,
            start = Offset(xPos, top),
            end = Offset(xPos, bottom),
            strokeWidth = lineWidth,
        )
    }
/**
 * Draws a horizontal line of [color] on the canvas at the [yPos] with a shadow of [shadowColor].
 * The line width will be determined by [left] and [right] positions.
 *
 * @since 200.7.0
 */
private fun DrawScope.drawHorizontalLineAndShadow(
    yPos: Float,
    left: Float,
    right: Float,
    lineColor: Color,
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
        color = lineColor,
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
 * @param labelTypography The typography to use for the text.
 * @param xPos The location where the text should be drawn.
 * @param color The color of the text.
 * @param shadowColor The color of the text shadow.
 * @param alignment The alignment of text relative to [xPos].
 * @since 200.7.0
 */
private fun DrawScope.drawText(
    text: String,
    textMeasurer: TextMeasurer,
    labelTypography: LabelTypography,
    xPos: Float,
    color: Color = Color.Black,
    shadowColor: Color = Color.White,
    shadowBlurRadius: Float,
    alignment: TextAlignment = TextAlignment.CENTER
) {
    val measuredText = textMeasurer.measure(
        text = text,
        style = labelTypography.labelStyle
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
        shadow = Shadow(
            color = shadowColor,
            offset = Offset(1f, 1f),
            blurRadius = shadowBlurRadius
        )
    )
}

/**
 * Draws the tick marks on the canvas of [tickHeight] with a [color] with a shadow of [shadowColor].
 *
 * The label of the tick marks will be drawn with a [textColor] and shadow of [textShadowColor]. The tickmark
 * position will be determined by [ScalebarDivision.xOffset].
 */
private fun DrawScope.drawTickMarksWithLabels(
    tickMarks: List<ScalebarDivision>,
    color: Color,
    shadowColor: Color,
    textMeasurer: TextMeasurer,
    labelTypography: LabelTypography,
    textColor: Color,
    textShadowColor: Color,
    textShadowBlurRadius: Float
) {

    for (i in 0 until tickMarks.size - 1) {
        drawVerticalLineAndShadow(
            xPos = tickMarks[i].xOffset.toFloat(),
            top = 0f,
            bottom = scalebarHeight,
            lineColor = color,
            shadowColor = shadowColor
        )
        drawText(
            text = tickMarks[i].label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = tickMarks[i].xOffset.toFloat(),
            color = textColor,
            shadowColor = textShadowColor,
            shadowBlurRadius = textShadowBlurRadius,
            alignment = if (i == 0) TextAlignment.RIGHT else TextAlignment.CENTER
        )
    }
}
