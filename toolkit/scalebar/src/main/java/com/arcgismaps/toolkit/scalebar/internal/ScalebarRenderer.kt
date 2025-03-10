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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.arcgismaps.toolkit.scalebar.internal.ScalebarUtils.toPx
import com.arcgismaps.toolkit.scalebar.theme.LabelTypography
import com.arcgismaps.toolkit.scalebar.theme.ScalebarColors
import com.arcgismaps.toolkit.scalebar.theme.ScalebarDefaults
import com.arcgismaps.toolkit.scalebar.theme.ScalebarShapes

private val pixelAlignment = 1.dp // Aligns the horizontal line edges
internal val lineWidth = 2.dp
private val shadowOffset = 1.dp
private val scalebarHeight = 7.dp // Height of the scalebar in pixels
internal const val labelXPadding = 4f // padding between scalebar labels

/**
 * Displays a line scalebar with single label and endpoint lines.
 *
 * @param modifier The modifier to apply to the layout.
 * @param displayLength The width of the scalebar in pixels.
 * @param label The scale value to display.
 * @param colorScheme The color scheme to use.
 * @param labelTypography The typography to use for the label.
 * @param shapes The shape properties to use.
 * @since 200.7.0
 */
@Composable
internal fun LineScalebar(
    modifier: Modifier = Modifier,
    displayLength: Double,
    label: String,
    colorScheme: ScalebarColors,
    labelTypography: LabelTypography,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryLabelHeightInDp = textMeasurer.getTextHeightInDp(label, labelTypography)

    val totalHeight = scalebarHeight + primaryLabelHeightInDp
    val totalWidth = displayLength.dp + shadowOffset + pixelAlignment + (lineWidth / 2)

    Canvas(
        modifier = Modifier
            .testTag("LineScalebar")
            .width(totalWidth)
            .height(totalHeight)
    ) {
        // Since the lineWidth is drawn from the center, we need to offset the line by half the lineWidth
        drawVerticalLineAndShadow(
            xPos = 0f + (lineWidth / 2).toPx(),
            top = 0f,
            bottom = scalebarHeight.toPx() + pixelAlignment.toPx(),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // bottom line
        drawHorizontalLineAndShadow(
            yPos = scalebarHeight.toPx(),
            left = 0f,
            right = displayLength.toPx(density),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )

        // right line
        drawVerticalLineAndShadow(
            xPos = displayLength.toPx(density) + (lineWidth / 2).toPx(),
            top = 0f,
            bottom = scalebarHeight.toPx() + pixelAlignment.toPx(),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // text label
        drawText(
            text = label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = displayLength.toPx(density) / 2,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius,
            alignment = TextAlignment.CENTER
        )
    }
}

/**
 * Displays bar scalebar with a single label.
 *
 * @param modifier The modifier to apply to the layout.
 * @param displayLength The width of the scale bar.
 * @param label The scale value to display.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 * @param labelTypography The typography to use for the label.
 * @since 200.7.0
 */
@Composable
internal fun BarScalebar(
    modifier: Modifier = Modifier,
    displayLength: Double,
    label: String,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    labelTypography: LabelTypography
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryLabelHeightInDp = textMeasurer.getTextHeightInDp(label, labelTypography)

    val totalHeight = scalebarHeight + primaryLabelHeightInDp
    val totalWidth = displayLength.dp + shadowOffset + pixelAlignment
    val topLeftPoint = Offset(0f + lineWidth.value, 0f + lineWidth.value)

    Canvas(
        modifier = Modifier
            .testTag("BarScalebar")
            .width(totalWidth)
            .height(totalHeight)
    ) {
        // draws the rectangle's shadow
        drawRoundRect(
            color = colorScheme.shadowColor,
            topLeft = Offset(topLeftPoint.x + shadowOffset.toPx(), topLeftPoint.y + shadowOffset.toPx()),
            size = Size(displayLength.toPx(density), scalebarHeight.toPx()),
            cornerRadius = CornerRadius(shapes.barCornerRadius),
            style = Stroke(width = lineWidth.toPx())
        )

        // Draws the rectangle's fill color
        drawRoundRect(
            color = colorScheme.fillColor,
            topLeft = topLeftPoint,
            cornerRadius = CornerRadius(shapes.barCornerRadius),
            size = Size(displayLength.toPx(density), scalebarHeight.toPx()),

            )
        // draws the rectangle's border
        drawRoundRect(
            color = colorScheme.lineColor,
            topLeft = topLeftPoint,
            size = Size(displayLength.toPx(density), scalebarHeight.toPx()),
            cornerRadius = CornerRadius(shapes.barCornerRadius),
            style = Stroke(width = lineWidth.toPx())
        )

        drawText(
            text = label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = displayLength.toPx(density) / 2.0f,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius,
            alignment = TextAlignment.CENTER
        )
    }
}

/**
 * Displays AlternatingBar scalebar with segmented bars of alternating fill color.
 *
 * @param modifier The modifier to apply to the layout.
 * @param displayLength The width of the scale bar.
 * @param scalebarDivisions The scale value to display.
 * @param colorScheme The color scheme to use.
 * @param shapes The shape properties to use.
 * @param labelTypography The typography to use for the label.
 * @since 200.7.0
 */
@Composable
internal fun AlternatingBarScalebar(
    modifier: Modifier = Modifier,
    displayLength: Double,
    scalebarDivisions: List<ScalebarDivision>,
    colorScheme: ScalebarColors,
    shapes: ScalebarShapes,
    labelTypography: LabelTypography
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryLabelHeightInDp = textMeasurer.getTextHeightInDp(scalebarDivisions.last().label, labelTypography)
    val primaryLabelWidthInDp = textMeasurer.getTextWidthInDp(scalebarDivisions.last().label, labelTypography)
    val totalHeight = scalebarHeight + primaryLabelHeightInDp
    val totalWidth = displayLength.dp + primaryLabelWidthInDp / 2
    val topLeftPoint = Offset(0f + lineWidth.value, 0f + lineWidth.value)

    Canvas(
        modifier = Modifier
            .testTag("AlternatingBarScalebar")
            .width(totalWidth)
            .height(totalHeight)
    ) {
        // draws the rectangle's shadow
        drawRoundRect(
            color = colorScheme.shadowColor,
            topLeft = Offset(topLeftPoint.x + shadowOffset.toPx(), topLeftPoint.y + shadowOffset.toPx()),
            size = Size(displayLength.toPx(density), scalebarHeight.toPx()),
            cornerRadius = CornerRadius(shapes.barCornerRadius),
            style = Stroke(width = lineWidth.toPx())
        )

        // Draws the alternating fill colors, bars and text labels
        for (index in 0 until scalebarDivisions.size - 1) {
            val startX = scalebarDivisions[index].xOffset.toPx(density)
            val endX = scalebarDivisions[index + 1].xOffset.toPx(density)
            val width = endX - startX

            // Draw the inner fill color
            drawRoundRect(
                color = if (index % 2 == 0) colorScheme.fillColor else colorScheme.alternateFillColor,
                topLeft = Offset(topLeftPoint.x + startX, topLeftPoint.y),
                size = Size(width, scalebarHeight.toPx()),
                cornerRadius = CornerRadius(shapes.barCornerRadius)
            )

            if (index != 0) {
                // Draw only the inner lines
                drawLine(
                    color = colorScheme.lineColor,
                    start = Offset(scalebarDivisions[index].xOffset.toPx(density) + topLeftPoint.x, topLeftPoint.y),
                    end = Offset(
                        scalebarDivisions[index].xOffset.toPx(density) + topLeftPoint.x,
                        scalebarHeight.toPx()
                    ),
                    strokeWidth = lineWidth.toPx(),
                )
            }

            drawText(
                text = scalebarDivisions[index].label,
                textMeasurer = textMeasurer,
                labelTypography = labelTypography,
                xPos = scalebarDivisions[index].xOffset.toPx(density),
                color = colorScheme.textColor,
                shadowColor = colorScheme.textShadowColor,
                shadowBlurRadius = shapes.textShadowBlurRadius,
                alignment = if (index == 0) TextAlignment.RIGHT else TextAlignment.CENTER
            )
        }

        // draw last text label
        drawText(
            text = scalebarDivisions.last().label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = scalebarDivisions.last().xOffset.toPx(density),
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius,
            alignment = TextAlignment.CENTER
        )

        // draws the rectangle's border
        drawRoundRect(
            color = colorScheme.lineColor,
            topLeft = topLeftPoint,
            size = Size(displayLength.toPx(density), scalebarHeight.toPx()),
            cornerRadius = CornerRadius(shapes.barCornerRadius),
            style = Stroke(width = lineWidth.toPx())
        )
    }
}

/**
 * Displays a graduated line scalebar with multiple labels and tick marks.
 *
 * @param modifier The modifier to apply to the layout.
 * @param displayLength The width of the scale bar.
 * @param tickMarks The list of tick marks to display.
 * @param colorScheme The color scheme to use.
 * @param labelTypography The typography to use for the label.
 * @param shapes The shape properties to use.
 * @since 200.7.0
 */
@Composable
internal fun GraduatedLineScalebar(
    modifier: Modifier = Modifier,
    displayLength: Double,
    tickMarks: List<ScalebarDivision>,
    colorScheme: ScalebarColors,
    labelTypography: LabelTypography,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryLabelHeightInDp = textMeasurer.getTextHeightInDp(tickMarks.last().label, labelTypography)
    val primaryLabelWidthInDp = textMeasurer.getTextWidthInDp(tickMarks.last().label, labelTypography)

    val totalHeight = scalebarHeight + primaryLabelHeightInDp
    val totalWidth = displayLength.dp + primaryLabelWidthInDp / 2

    Canvas(
        modifier = Modifier
            .testTag("GraduatedLineScalebar")
            .width(totalWidth)
            .height(totalHeight)
    ) {
        val tickMarksWithPixelOffsets = tickMarks.map { tickMark ->
            tickMark.copy(xOffset = tickMark.xOffset.toPx(density).toDouble())
        }
        // draw tick marks
        drawTickMarksWithLabels(
            tickMarks = tickMarksWithPixelOffsets,
            color = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            textColor = colorScheme.textColor,
            textShadowColor = colorScheme.textShadowColor,
            textShadowBlurRadius = shapes.textShadowBlurRadius
        )

        // bottom line
        drawHorizontalLineAndShadow(
            yPos = scalebarHeight.toPx(),
            left = 0f,
            right = displayLength.toPx(density) + (lineWidth / 2).toPx(),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
    }
}

/**
 * Displays a dual unit line scalebar with scale length and measurements in primary and
 * alternate unit.
 *
 * @param modifier The modifier to apply to the layout.
 * @param primaryScalebarDivision The end segment for the primary unit.
 * @param alternateScalebarDivision The end segment for the alternate unit.
 * @param colorScheme The color scheme to use.
 * @param labelTypography The typography to use for the labels.
 * @param shapes The shape properties to use.
 *
 * @since 200.7.0
 */
@Composable
internal fun DualUnitLineScalebar(
    modifier: Modifier = Modifier,
    primaryScalebarDivision: ScalebarDivision,
    alternateScalebarDivision: ScalebarDivision,
    colorScheme: ScalebarColors,
    labelTypography: LabelTypography,
    shapes: ScalebarShapes
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryLabelHeightInDp = textMeasurer.getTextHeightInDp(primaryScalebarDivision.label, labelTypography)
    val primaryLabelWidthInDp = textMeasurer.getTextWidthInDp(primaryScalebarDivision.label, labelTypography)
    val alternateLabelHeightInDp = textMeasurer.getTextHeightInDp(alternateScalebarDivision.label, labelTypography)
    val alternateLabelWidthInDp = textMeasurer.getTextWidthInDp(alternateScalebarDivision.label, labelTypography)

    val totalHeight: Dp = primaryLabelHeightInDp + scalebarHeight * 2 + alternateLabelHeightInDp
    val totalWidth = max(
        (primaryLabelWidthInDp / 2) + primaryScalebarDivision.xOffset.dp,
        (alternateLabelWidthInDp / 2) + alternateScalebarDivision.xOffset.dp
    )
    val scalebarHeightInPx = with(density) { scalebarHeight.toPx() }

    Canvas(
        modifier = Modifier
            .testTag("DualUnitLineScalebar")
            .width(totalWidth)
            .height(totalHeight)
    ) {
        // left end line
        drawVerticalLineAndShadow(
            xPos = 0f + (lineWidth / 2).toPx(),
            top = primaryLabelHeightInDp.toPx(),
            bottom = scalebarHeightInPx * 2 + primaryLabelHeightInDp.toPx(),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // Scalebar line
        drawHorizontalLineAndShadow(
            yPos = primaryLabelHeightInDp.toPx() + scalebarHeightInPx,
            left = 0f,
            right = primaryScalebarDivision.xOffset.toPx(density),
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // right end line
        drawVerticalLineAndShadow(
            xPos = primaryScalebarDivision.xOffset.toPx(density),
            top = primaryLabelHeightInDp.toPx(),
            bottom = primaryLabelHeightInDp.toPx() + scalebarHeightInPx,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // draw primary label
        drawText(
            text = primaryScalebarDivision.label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = primaryScalebarDivision.xOffset.toPx(density),
            yPos = 0f,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius,
            alignment = TextAlignment.CENTER
        )
        // draw end line for alternate scalebar
        drawVerticalLineAndShadow(
            xPos = alternateScalebarDivision.xOffset.toPx(density),
            top = primaryLabelHeightInDp.toPx() + scalebarHeightInPx + (lineWidth / 2).toPx(),
            bottom = primaryLabelHeightInDp.toPx() + scalebarHeightInPx * 2,
            lineColor = colorScheme.lineColor,
            shadowColor = colorScheme.shadowColor
        )
        // draw alternate unit label
        drawText(
            text = alternateScalebarDivision.label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = alternateScalebarDivision.xOffset.toPx(density),
            yPos = primaryLabelHeightInDp.toPx() + scalebarHeightInPx * 2,
            color = colorScheme.textColor,
            shadowColor = colorScheme.textShadowColor,
            shadowBlurRadius = shapes.textShadowBlurRadius,
            alignment = TextAlignment.CENTER
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun LineScaleBarPreview() {
    val displayLength = 160.0
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        LineScalebar(
            modifier = Modifier,
            displayLength = displayLength,
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
    val displayLength = 160.0
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        BarScalebar(
            modifier = Modifier,
            displayLength = displayLength,
            label = "1000 km",
            colorScheme = ScalebarDefaults.colors(shadowColor = Color.Red, textShadowColor = Color.Red),
            shapes = ScalebarDefaults.shapes(barCornerRadius = 4f, textShadowBlurRadius = 2f),
            labelTypography = ScalebarDefaults.typography()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun DualUnitLineScalebarPreview() {
    val displayLength = 139.3
    val primaryScalebarDivision = ScalebarDivision(displayLength, "1750 mi")
    val alternateScalebarDivision = ScalebarDivision(0.75 * displayLength, "2500 Km")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        DualUnitLineScalebar(
            modifier = Modifier,
            primaryScalebarDivision = primaryScalebarDivision,
            alternateScalebarDivision = alternateScalebarDivision,
            colorScheme = ScalebarDefaults.colors(),
            labelTypography = ScalebarDefaults.typography(),
            shapes = ScalebarDefaults.shapes()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun GraduatedLineScaleBarPreview() {
    val displayLength = 139.3
    val tickMarks = listOf(
        ScalebarDivision(0.0, "0"),
        ScalebarDivision(0.33 * displayLength, "100"),
        ScalebarDivision(0.66 * displayLength, "200"),
        ScalebarDivision(displayLength, "300 km")
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        GraduatedLineScalebar(
            modifier = Modifier,
            displayLength = displayLength,
            colorScheme = ScalebarDefaults.colors(),
            tickMarks = tickMarks,
            labelTypography = ScalebarDefaults.typography(),
            shapes = ScalebarDefaults.shapes()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun AlternatingBarScaleBarPreview() {
    val displayLength = 139.3
    val scalebarDivisions = listOf(
        ScalebarDivision(0.0, "0"),
        ScalebarDivision(0.33 * displayLength, "100"),
        ScalebarDivision(0.66 * displayLength, "200"),
        ScalebarDivision(displayLength, "300 km")
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), contentAlignment = Alignment.BottomStart
    ) {
        AlternatingBarScalebar(
            modifier = Modifier,
            displayLength = displayLength,
            colorScheme = ScalebarDefaults.colors(),
            scalebarDivisions = scalebarDivisions,
            labelTypography = ScalebarDefaults.typography(),
            shapes = ScalebarDefaults.shapes()
        )
    }
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
        start = Offset(xPos + shadowOffset.toPx(), top),
        end = Offset(xPos + shadowOffset.toPx(), bottom),
        strokeWidth = lineWidth.toPx(),
    )
    drawLine(
        color = lineColor,
        start = Offset(xPos, top),
        end = Offset(xPos, bottom),
        strokeWidth = lineWidth.toPx(),
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
        start = Offset((left + pixelAlignment.toPx()) + shadowOffset.toPx(), yPos + shadowOffset.toPx()),
        end = Offset((right + pixelAlignment.toPx()) + shadowOffset.toPx(), yPos + shadowOffset.toPx()),
        strokeWidth = lineWidth.toPx(),
    )
    drawLine(
        color = lineColor,
        start = Offset(left + pixelAlignment.toPx(), yPos),
        end = Offset(right + pixelAlignment.toPx(), yPos),
        strokeWidth = lineWidth.toPx(),
    )
}

/**
 * Draws the text on the canvas with a shadow.
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
    yPos: Float = scalebarHeight.toPx(),
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
        TextAlignment.LEFT -> xPos - measuredText.size.width + pixelAlignment.toPx()
        TextAlignment.CENTER -> xPos - (measuredText.size.width / 2)
        TextAlignment.RIGHT -> xPos - pixelAlignment.toPx()
    }
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
 * Draws tick marks of size [scalebarHeight] the canvas with the specified [color] and [shadowColor].
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

    for (i in tickMarks.indices) {
        drawVerticalLineAndShadow(
            xPos = tickMarks[i].xOffset.toFloat() + (lineWidth / 2).toPx(),
            top = 0f,
            bottom = scalebarHeight.toPx() + shadowOffset.toPx(),
            lineColor = color,
            shadowColor = shadowColor
        )
        drawText(
            text = tickMarks[i].label,
            textMeasurer = textMeasurer,
            labelTypography = labelTypography,
            xPos = tickMarks[i].xOffset.toFloat() + (lineWidth / 2).toPx(),
            color = textColor,
            shadowColor = textShadowColor,
            shadowBlurRadius = textShadowBlurRadius,
            alignment = if (i == 0) TextAlignment.RIGHT else TextAlignment.CENTER
        )
    }
}

/**
 * Measures the height of the text in dp.
 *
 * @param text The text to measure.
 * @param labelTypography The typography to use for the text.
 * @since 200.7.0
 */
@Composable
private fun TextMeasurer.getTextHeightInDp(
    text: String,
    labelTypography: LabelTypography,
): Dp {
    val density = LocalDensity.current

    val textHeightInPx = this.measure(
        text = text,
        style = labelTypography.labelStyle
    ).size.height

    return with(density) { textHeightInPx.toDp() }
}

/**
 * Measures the height of the text in dp.
 *
 * @param text The text to measure.
 * @param labelTypography The typography to use for the text.
 * @since 200.7.0
 */
@Composable
private fun TextMeasurer.getTextWidthInDp(
    text: String,
    labelTypography: LabelTypography,
): Dp {
    val density = LocalDensity.current

    val textHeightInPx = this.measure(
        text = text,
        style = labelTypography.labelStyle
    ).size.width

    return with(density) { textHeightInPx.toDp() }
}
