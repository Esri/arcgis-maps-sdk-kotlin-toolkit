/*
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.zIndex
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView

public class MapViewScope(internal val mapView: MapView)

@Composable
//our warning annotation here.
public fun MapViewScope.Callout(
    point: Point,
    modifier: Modifier = Modifier.zIndex(1f),
    content: @Composable MapViewScope.() -> Unit
) {
    val screenLocation by remember(point) { mutableStateOf(mapView.locationToScreen(point)) }
    println("foo screen location ${screenLocation.x} ${screenLocation.y}")
    var currentCoordinates: IntOffset by remember(point) { mutableStateOf(IntOffset(screenLocation.x.toInt(), screenLocation.y.toInt())) }
        val popupPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect,
                                       windowSize: IntSize,
                                       layoutDirection: LayoutDirection,
                                       popupContentSize: IntSize
        ): IntOffset {
            println("foo anchorBounds rect: $anchorBounds window size $windowSize ppupcontent size $popupContentSize")
            return currentCoordinates.copy(
                x = currentCoordinates.x - popupContentSize.width/2,
                y = currentCoordinates.y + anchorBounds.top - popupContentSize.height - 20
            )
        }
    }
    Popup(popupPositionProvider) {

        content()
    }

}

