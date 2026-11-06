/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol

internal object ImageDefaults {
    private const val CAMERA_LOCATION_SYMBOL_SIZE = 15.0f
    val selectedFootprintFillColor = Color.fromRgba(255, 0, 0, 128)
    val selectedFootprintOutlineColor = Color.red
    val additionalFootprintColor = Color.fromRgba(0, 0, 255, 64)
    val unselectedFootprintOutlineColor = Color.blue

    private fun symbolOutline() = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.white, 1f)

    fun activeCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.red,
        CAMERA_LOCATION_SYMBOL_SIZE
    ).apply { outline = symbolOutline() }

    fun additionalCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.fromRgba(0, 0, 255, 200),
        CAMERA_LOCATION_SYMBOL_SIZE
    ).apply { outline = symbolOutline() }

    fun inputLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.X,
        Color.red,
        CAMERA_LOCATION_SYMBOL_SIZE
    ).apply { outline = symbolOutline() }

    fun noImageMarkerSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.fromRgba(211, 211, 211, 255),
        12f
    ).apply { outline = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f) }
}

internal data class ViewerAppearance(
    val selectedFootprintFillColor: Color = ImageDefaults.selectedFootprintFillColor,
    val selectedFootprintOutlineColor: Color = ImageDefaults.selectedFootprintOutlineColor,
    val unselectedFootprintFillColor: Color = ImageDefaults.additionalFootprintColor,
    val unselectedFootprintOutlineColor: Color = ImageDefaults.unselectedFootprintOutlineColor,
    val searchPointMarkerSymbol: Symbol = ImageDefaults.inputLocationSymbol(),
    val noImageMarkerSymbol: Symbol = ImageDefaults.noImageMarkerSymbol(),
    val selectedCameraMarkerSymbol: Symbol = ImageDefaults.activeCameraLocationSymbol(),
    val allCamerasMarkerSymbol: Symbol = ImageDefaults.additionalCameraLocationSymbol()
)
