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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val scalebarHeight = 20f // Height of the scalebar in pixels

@Composable
public fun Scalebar(viewModel: ScalebarInterface) {
    val text = viewModel.someProperty.collectAsState()
    Text(text = text.value)
}

/**
 * Displays a single label with endpoint lines.
 *
 * @param modifier The modifier to apply to the layout.
 * @param scaleValue The scale value to display.
 * @param width The width of the scale bar.
 * @param lineColor The color of the scale bar lines.
 * @param shadowColor The color of the scale bar shadows.
 * @param textColor The color of the scale bar text.
 * @param textShadowColor The color of the scale bar text shadow.
 * @since 200.7.0
 */
@Composable
internal fun LineScalebar(
    modifier: Modifier = Modifier.testTag("LineScalebar"),
    scaleValue: String,
    width: Float = 300f,
    lineColor: Color = Color.Black,
    shadowColor: Color = Color.Unspecified,
    textColor: Color = Color.Black,
    textShadowColor: Color = Color.Unspecified
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(8.dp)
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

@Preview(showBackground = true, backgroundColor = 0xff91d2ff)
@Composable
internal fun LineScaleBarPreview() {
    LineScalebar(
        scaleValue = "1,000 km",
        lineColor = Color.White,
        shadowColor = Color.Gray,
        textColor = Color.Black,
        textShadowColor = Color.White
    )
}
