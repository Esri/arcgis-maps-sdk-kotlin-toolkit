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

package com.arcgismaps.toolkit.orientedimageryviewer

import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle

internal object ImageDefaults {
	private const val CAMERA_LOCATION_SYMBOL_SIZE = 15.0f
	private val ACTIVE_CAMERA_SYMBOL_COLOR = Color.fromRgba(255, 102, 102, 100)
	private val ADDITIONAL_CAMERA_SYMBOL_COLOR = Color.fromRgba(0, 128, 192, 100)

	val ADDITIONAL_FOOTPRINT_COLOR = Color.fromRgba(0, 128, 192, 60)

	fun activeCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
		SimpleMarkerSymbolStyle.Circle,
		ACTIVE_CAMERA_SYMBOL_COLOR,
		CAMERA_LOCATION_SYMBOL_SIZE
	)

	fun additionalCameraLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
		SimpleMarkerSymbolStyle.Circle,
		ADDITIONAL_CAMERA_SYMBOL_COLOR,
		CAMERA_LOCATION_SYMBOL_SIZE
	)

	fun inputLocationSymbol(): SimpleMarkerSymbol = SimpleMarkerSymbol(
		SimpleMarkerSymbolStyle.X,
		Color.red,
		CAMERA_LOCATION_SYMBOL_SIZE
	)
}