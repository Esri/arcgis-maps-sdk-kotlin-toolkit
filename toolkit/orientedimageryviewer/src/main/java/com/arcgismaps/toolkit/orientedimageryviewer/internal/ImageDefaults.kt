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
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle

internal object ImageDefaults {
    private const val CAMERA_LOCATION_SYMBOL_SIZE = 15.0f
    private val activeCameraSymbolColor = Color.fromRgba(255, 102, 102, 100)
    private val additionalCameraSymbolColor = Color.fromRgba(0, 128, 192, 100)

    val additionalFootprintColor = Color.fromRgba(0, 128, 192, 60)

    fun activeCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        activeCameraSymbolColor,
        CAMERA_LOCATION_SYMBOL_SIZE
    )

    fun additionalCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        additionalCameraSymbolColor,
        CAMERA_LOCATION_SYMBOL_SIZE
    )

    fun inputLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.X,
        Color.red,
        CAMERA_LOCATION_SYMBOL_SIZE
    )

    fun defaultMarkerSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle, Color.fromRgba(255, 0, 122, 194), 10f
    )
}
