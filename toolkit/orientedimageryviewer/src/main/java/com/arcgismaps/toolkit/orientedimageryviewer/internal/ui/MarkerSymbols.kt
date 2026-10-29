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

import androidx.annotation.StringRes
import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.toolkit.orientedimageryviewer.R

/** Identity is independent of symbol appearance and palette ordering. */
internal enum class MarkerOptionId { VioletSquare, YellowTriangle, OrangeDiamond }

internal data class MarkerOption(
    val id: MarkerOptionId,
    val symbol: SimpleMarkerSymbol,
    @StringRes val descriptionResId: Int
)

/** Symbol prototypes are cloned when selected so existing markers retain their appearance. */
internal object MarkerSymbols {
    val options: List<MarkerOption> = listOf(
        MarkerOption(
            MarkerOptionId.VioletSquare,
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Square, Color.fromRgba(238, 130, 238), 12f),
            R.string.marker_symbol_violet_square
        ),
        MarkerOption(
            MarkerOptionId.YellowTriangle,
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Triangle, Color.yellow, 12f),
            R.string.marker_symbol_yellow_triangle
        ),
        MarkerOption(
            MarkerOptionId.OrangeDiamond,
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.fromRgba(255, 165, 0), 12f),
            R.string.marker_symbol_orange_diamond
        )
    ).onEach { it.symbol.outline = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f) }

    val symbols: List<SimpleMarkerSymbol> = options.map { it.symbol }
}
