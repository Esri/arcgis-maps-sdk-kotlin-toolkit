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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A model for a simple color picker.
 *
 * @since 200.6.0
 */
internal object TraceColors {
    val colors = listOf(
        Color.Green,
        Color.Blue,
        Color.Red,
        Color.Yellow,
        Color.Black
    )

    private var chosenColor: Int = 0

    /**
     * Returns the next color in the sequence.
     *
     * @since 200.6.0
     */
    @Suppress("unused")
    internal fun nextColor(): Color {
        return colors[chosenColor].also {
            chosenColor = (chosenColor+1).mod(colors.size)
        }
    }

    private val sweepColors = mutableListOf(colors.last()).also {it.addAll(colors)}

    /**
     * A composable for use as an Icon or Button which provides a gradient ring
     * spanning the colors in colorMap.
     *
     * @param backgroundFill a color for use as the background of the ring.
     * @param modifier the modifier.
     * @Since 200.6.0
     */
    @Composable
    fun SpectralRing(backgroundFill: Color, modifier: Modifier = Modifier) {
        Box(modifier = modifier.clip(CircleShape).background(backgroundFill)) {
            val bgColor = MaterialTheme.colorScheme.background
            val brush = remember { Brush.sweepGradient(sweepColors) }
            Canvas(modifier = Modifier.aspectRatio(1f)) {
                val canvasWidth = size.width

                drawCircle(
                    brush = brush,
                    style = Stroke(canvasWidth * 0.13f),
                    radius = canvasWidth * .45f
                )
                drawCircle(
                    color = bgColor,
                    style = Stroke(canvasWidth * 0.05f),
                    radius = canvasWidth * .40f
                )
            }
        }
    }
}

@Preview
@Composable
private fun SpectrumRingPreview() {
    TraceColors.SpectralRing(Color.Red, modifier = Modifier.size(100.dp))
}
