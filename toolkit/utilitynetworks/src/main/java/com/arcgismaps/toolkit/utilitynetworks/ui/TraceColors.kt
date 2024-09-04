/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview

/**
 * A model for a simple color picker. Also Provides a composable for use as an Icon or Button.
 *
 * @since 200.6.0
 */
internal object TraceColors {
    private val colorMap = mapOf(
        0 to Color.Green,
        1 to Color.Blue,
        2 to Color.Red,
        3 to Color.Yellow,
        4 to Color.Black
    )

    val colors: List<Color> = colorMap.values.toList()

    private var chosenColor: Int = 0

    /**
     * For automatic choosing of colors for trace results
     *
     * @since 200.6.0
     */
    @Suppress("unused")
    internal fun nextColor(): Color {
        return colorMap[chosenColor]!!.also {
            chosenColor = (chosenColor+1).mod(colorMap.size)
        }
    }

    private val sweepColors = mutableListOf(colors.last()).also {it.addAll(colors)}
    private val brush = Brush.sweepGradient(sweepColors)

    @Composable
    fun SpectrumRing(modifier: Modifier = Modifier) {
        val bgColor = MaterialTheme.colorScheme.background
        Canvas(modifier = modifier.aspectRatio(1f)) {
            val canvasWidth = size.width

            drawCircle(
                brush = brush,
                style = Stroke(canvasWidth * 0.05f),
                radius = canvasWidth * .47f
            )
            drawCircle(
                color = bgColor,
                style = Stroke(canvasWidth * 0.05f),
                radius = canvasWidth * .42f
            )
        }
    }
}

@Preview
@Composable
private fun SpectrumRingPreview() {
    TraceColors.SpectrumRing()
}