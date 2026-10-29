/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle

/** Screen-space rendering keeps panoramic symbols upright and the same size as raster symbols. */
internal fun DrawScope.drawMarkerSymbol(
    symbol: SimpleMarkerSymbol,
    center: Offset,
    diameter: Float = symbol.size.dp.toPx()
) {
    if (!diameter.isFinite() || diameter <= 0f) return
    val half = diameter / 2f
    val open = symbol.style == SimpleMarkerSymbolStyle.Cross || symbol.style == SimpleMarkerSymbolStyle.X
    val path = Path().apply {
        when (symbol.style) {
            SimpleMarkerSymbolStyle.Circle -> addOval(Rect(center - Offset(half, half), center + Offset(half, half)))

            SimpleMarkerSymbolStyle.Triangle -> {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y + half)
                lineTo(center.x - half, center.y + half)
                close()
            }

            SimpleMarkerSymbolStyle.Diamond -> {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half)
                lineTo(center.x - half, center.y)
                close()
            }

            SimpleMarkerSymbolStyle.Square -> addRect(Rect(center - Offset(half, half), center + Offset(half, half)))

            SimpleMarkerSymbolStyle.Cross -> {
                moveTo(center.x - half, center.y)
                lineTo(center.x + half, center.y)
                moveTo(center.x, center.y - half)
                lineTo(center.x, center.y + half)
            }

            SimpleMarkerSymbolStyle.X -> {
                moveTo(center.x - half, center.y - half)
                lineTo(center.x + half, center.y + half)
                moveTo(center.x + half, center.y - half)
                lineTo(center.x - half, center.y + half)
            }
        }
    }
    val lineWidth = 2.dp.toPx()
    symbol.outline?.takeIf { it.width > 0f }?.let { outline ->
        if (open) drawPath(path, Color(outline.color.argb), style = Stroke(lineWidth + 2 * outline.width.dp.toPx()))
    }
    drawPath(path, Color(symbol.color.argb), style = if (open) Stroke(lineWidth) else Fill)
    if (!open) symbol.outline?.takeIf { it.width > 0f }?.let { outline ->
        drawPath(path, Color(outline.color.argb), style = Stroke(outline.width.dp.toPx()))
    }
}
