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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.arcgismaps.Color as SymbolColor

/** Each graphic receives its own symbol so later selections cannot change existing markers. */
internal object MarkerSymbols {
    val symbols: List<SimpleMarkerSymbol> = listOf(
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Square, SymbolColor.fromRgba(238, 130, 238), 12f),
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Triangle, SymbolColor.yellow, 12f),
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, SymbolColor.fromRgba(255, 165, 0), 12f)
    ).onEach { it.outline = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, SymbolColor.black, 1f) }
}

@Composable
internal fun MarkerSelection(
    currentMarkerSymbol: SimpleMarkerSymbol,
    onSelectSymbol: (SimpleMarkerSymbol) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    val descriptions = listOf(
        stringResource(R.string.marker_symbol_violet_square),
        stringResource(R.string.marker_symbol_yellow_triangle),
        stringResource(R.string.marker_symbol_orange_diamond)
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.marker_selection_back))
                }
                Text(stringResource(R.string.marker_selection_title), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.toolbar_close))
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarkerSymbols.symbols.forEachIndexed { index, symbol ->
                    Canvas(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = currentMarkerSymbol.style == symbol.style,
                                role = Role.RadioButton,
                                onClick = { onSelectSymbol(symbol) }
                            )
                            .semantics { contentDescription = descriptions[index] }
                    ) {
                        drawMarkerSymbol(symbol, center, 24.dp.toPx())
                    }
                }
            }
        }
    }
}

/** Screen-space rendering keeps panoramic symbols upright and the same size as raster symbols. */
internal fun DrawScope.drawMarkerSymbol(
    symbol: SimpleMarkerSymbol,
    center: Offset,
    diameter: Float = symbol.size.dp.toPx()
) {
    val half = diameter / 2f
    val path = Path().apply {
        when (symbol.style) {
            SimpleMarkerSymbolStyle.Triangle -> {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y + half)
                lineTo(center.x - half, center.y + half)
            }

            SimpleMarkerSymbolStyle.Diamond -> {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half)
                lineTo(center.x - half, center.y)
            }

            else -> {
                moveTo(center.x - half, center.y - half)
                lineTo(center.x + half, center.y - half)
                lineTo(center.x + half, center.y + half)
                lineTo(center.x - half, center.y + half)
            }
        }
        close()
    }
    drawPath(path, Color(symbol.color.argb))
    symbol.outline?.let { outline ->
        drawPath(path, Color(outline.color.argb), style = Stroke(outline.width.dp.toPx()))
    }
}
